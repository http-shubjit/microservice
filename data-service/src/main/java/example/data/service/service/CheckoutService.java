package example.data.service.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import example.data.service.entity.CartItem;
import example.data.service.entity.Order;
import example.data.service.entity.UserBudget;
import example.data.service.repository.CartItemRepository;
import example.data.service.repository.OrderRepository;
import example.data.service.repository.UserBudgetRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    @Autowired
    private CartItemRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserBudgetRepository budgetRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Phase 1: Create Razorpay Order AND save a PENDING order in our DB
     */
    @Transactional
    public String createRazorpayOrder(String email, List<CartItem> selectedCartItemIds) throws Exception {
        long totalInPaise = 0;
        double totalInRupees = 0;

        if (selectedCartItemIds == null || selectedCartItemIds.size() <= 0) {
            throw new RuntimeException("No items selected.");

        }
        else {
            for (CartItem item : selectedCartItemIds) {
                if (!item.getUserEmail().equalsIgnoreCase(email)) {
                    throw new RuntimeException("Unauthorized cart item.");
                }
                totalInPaise += (long) (item.getPrice() * item.getQuantity() * 100);
                totalInRupees += (item.getPrice() * item.getQuantity());
            }
}
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());
        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);
        String rzpOrderId = razorpayOrder.get("id");
        System.out.println("razorpayOrder" + razorpayOrder);
        System.out.println("rzpOrderId"+rzpOrderId);
    // CREATE PENDING ORDER RECORD IMMEDIATELY
        Order pendingOrder = Order.builder()
                .userEmail(email)
                .totalAmount(totalInRupees)
                .status("PENDING") // Marked as pending
                .razorpayOrderId(rzpOrderId)
                .build();
        orderRepository.save(pendingOrder);

        return rzpOrderId;
    }

    /**
     * Phase 2: Verify payment and update status from PENDING to PAID
     */
    @Transactional
    public boolean verifyAndFulfill(String email, String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                Order existingOrder = orderRepository.findByRazorpayOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Order record not found"));
                existingOrder.setStatus("PAID");
                orderRepository.save(existingOrder);

                // Clear purchased items from cart
               // cartRepository.deleteAllById(purchasedCartItemIds);

                // Update budget
                Optional<UserBudget> userBudget = budgetRepository.findByEmail(email);
                if (userBudget.isPresent()) {
                    UserBudget budget = userBudget.get();
                    budget.setMonthlyExpense(budget.getMonthlyExpense() + existingOrder.getTotalAmount());
                    budgetRepository.save(budget);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Phase 3: Explicitly mark an order as FAILED if payment drops
     */
    @Transactional
    public void markOrderAsFailed(String orderId) {
        orderRepository.findByRazorpayOrderId(orderId).ifPresent(order -> {
            order.setStatus("FAILED");
            orderRepository.save(order);
        });
    }
}