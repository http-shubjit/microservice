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
     * Phase 1: Create a Razorpay Order for SPECIFIC cart items
     */
    public String createRazorpayOrder(String email, List<Long> selectedCartItemIds) throws Exception {

        if (selectedCartItemIds == null || selectedCartItemIds.isEmpty()) {
            throw new RuntimeException("No items selected for checkout.");
        }

        // 1. Fetch ONLY the items the user selected using standard JPA findAllById
        List<CartItem> selectedItems = cartRepository.findAllById(selectedCartItemIds);

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Selected items could not be found.");
        }

        // Calculate total for ONLY selected items
        long totalInPaise = 0;
        for (CartItem item : selectedItems) {
            // Security Check: Ensure the requested item actually belongs to this user
            if (!item.getUserEmail().equalsIgnoreCase(email)) {
                throw new RuntimeException("Unauthorized attempt to purchase items belonging to another user.");
            }
            totalInPaise += (long) (item.getPrice() * item.getQuantity() * 100);
        }

        // Initialize Razorpay Client
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        // Build the payload
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        // Generate Razorpay Order
        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        return razorpayOrder.get("id");
    }

    /**
     * Phase 2: Verify payment and fulfill ONLY the purchased items
     */
    @Transactional
    public boolean verifyAndFulfill(String email, String orderId, String paymentId, String signature,
            double actualAmount, List<Long> purchasedCartItemIds) {
        try {
            // Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                // Create permanent Order record
                Order newOrder = Order.builder()
                        .userEmail(email)
                        .totalAmount(actualAmount)
                        .status("PAID")
                        .build();

                orderRepository.save(newOrder);

                // 2. Clear ONLY the specific items that were successfully purchased
                if (purchasedCartItemIds != null && !purchasedCartItemIds.isEmpty()) {
                    cartRepository.deleteAllById(purchasedCartItemIds);
                }

                // Update their Budget
                Optional<UserBudget> userBudget = budgetRepository.findByEmail(email);
                if (userBudget.isPresent()) {
                    UserBudget budget = userBudget.get();
                    budget.setMonthlyExpense(budget.getMonthlyExpense() + actualAmount);
                    budgetRepository.save(budget);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Payment verification failed: " + e.getMessage());
            return false;
        }
    }
}