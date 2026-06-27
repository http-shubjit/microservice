package example.data.service.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import example.data.service.entity.CartItem;
import example.data.service.entity.Order;
import example.data.service.entity.OrderItem;
import example.data.service.repository.CartItemRepository;
import example.data.service.repository.OrderRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    @Autowired
    private CartItemRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public String createRazorpayOrder(String email, List<CartItem> selectedCartItems) throws Exception {
        if (selectedCartItems == null || selectedCartItems.isEmpty()) {
            throw new RuntimeException("No items selected.");
        }

        long totalInPaise = 0;
        double totalInRupees = 0;

        for (CartItem item : selectedCartItems) {
            if (!item.getUserEmail().equalsIgnoreCase(email)) {
                throw new RuntimeException("Unauthorized cart item.");
            }
            totalInPaise += (long) (item.getPrice() * item.getQuantity() * 100);
            totalInRupees += (item.getPrice() * item.getQuantity());
        }

        // Create Razorpay Order
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());
        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
        String rzpOrderId = razorpayOrder.get("id");

        // 1. Create Parent Order object
        Order pendingOrder = Order.builder()
                .userEmail(email)
                .totalAmount(totalInRupees)
                .status("PENDING")
                .razorpayOrderId(rzpOrderId)
                .build();

        // 2. Map the 3 CartItems into 3 OrderItems
        List<OrderItem> orderItems = selectedCartItems.stream().map(cartItem -> {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId()) // Maps your variables
                    .productTitle(cartItem.getProductTitle())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .order(pendingOrder) // <--- CRITICAL: Passes the parent reference to the child item
                    .build();
            return orderItem;
        }).collect(Collectors.toList());

        // 3. Set the virtual list in the parent
        pendingOrder.setItems(orderItems);

        // 4. Save parent order. Java saves the order, gets the order_id,
        // and injects it into all 3 order_items records automatically!
        orderRepository.save(pendingOrder);

        return rzpOrderId;
    }

    @Transactional
    public boolean verifyAndFulfill(String email, String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                // 1. Fetch the order by its Razorpay ID
                Order existingOrder = orderRepository.findByRazorpayOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Order record not found"));

                existingOrder.setStatus("PAID");
                orderRepository.save(existingOrder);

                // 2. Clear out the items from the cart since they are paid for
                // Loop through the 3 items stored in our Order history memory
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