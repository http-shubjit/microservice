package example.data.service.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.entity.*;
import example.data.service.repository.CartItemRepository;
import example.data.service.repository.OrderRepository;
import example.data.service.repository.ShippingAddressRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    @Autowired
    private CartItemRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShippingAddressRepository shippingAddressRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public Map<String, Object> createRazorpayOrderFromCart(String email) throws Exception {

        List<CartItem> cartItems = cartRepository.findByUserEmail(email);

        if (cartItems == null || cartItems.isEmpty()) {
            return Map.of("error", "Cart is empty. Cannot create order.");
        }

        long totalInPaise = 0;
        double totalInRupees = 0;

        for (CartItem item : cartItems) {
            totalInPaise += (long) (item.getPrice() * item.getQuantity() * 100);
            totalInRupees += (item.getPrice() * item.getQuantity());
        }

        String productTitle = cartItems.stream()
                .map(i -> i.getProductTitle() + " x" + i.getQuantity())
                .collect(Collectors.joining(", "));

        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
        String rzpOrderId = razorpayOrder.get("id");

        Order pendingOrder = Order.builder()
                .userEmail(email)
                .totalAmount(totalInRupees)
                .status("PENDING")
                .razorpayOrderId(rzpOrderId)
                .build();

        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> OrderItem.builder()
                .productId(cartItem.getProductId())
                .productTitle(cartItem.getProductTitle())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .order(pendingOrder)
                .build()).collect(Collectors.toList());

        pendingOrder.setItems(orderItems);
        orderRepository.save(pendingOrder);

        Map<String, Object> result = new HashMap<>();
        result.put("razorpayOrderId", rzpOrderId);
        result.put("amount", totalInPaise);
        result.put("amountDisplay", "₹" + String.format("%.2f", totalInRupees));
        result.put("productTitle", productTitle);
        return result;
    }

    @Transactional
    public boolean verifyAndFulfill(String email, PaymentVerificationRequest request) {
        try {
            // 1. Verify Razorpay signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                // 2. Fetch and mark order as PAID
                Order existingOrder = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                existingOrder.setStatus("PAID");
                orderRepository.save(existingOrder);

                // 3. Save shipping address linked to this order
                ShippingAddress shipping = ShippingAddress.builder()
                        .order(existingOrder)
                        .fullName(request.getFullName())
                        .phone(request.getPhone())
                        .address(request.getAddress())
                        .city(request.getCity())
                        .pincode(request.getPincode())
                        .build();

                shippingAddressRepository.save(shipping);

                // 4. Clear cart
                for (OrderItem item : existingOrder.getItems()) {
                    cartRepository.deleteByUserEmailAndProductId(email, item.getProductId());
                }

                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}