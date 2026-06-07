package example.data.service.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 1. FIXED: Correct Razorpay Utils import
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;

import example.data.service.entity.CartItem;
import example.data.service.entity.Order; // Your Database Order
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
     * Phase 1: Create a Razorpay Order
     */
    public String createRazorpayOrder(String email) throws Exception {
        List<CartItem> cart = cartRepository.findByUserEmail(email);

        if (cart.isEmpty()) {
            throw new RuntimeException("Your cart is empty!");
        }

        // Calculate total. Razorpay expects amounts in PAISE (1 INR = 100 Paise)
        long totalInPaise = 0;
        for (CartItem item : cart) {
            totalInPaise += (long) (item.getPrice() * item.getQuantity() * 100);
        }

        // Initialize Razorpay Client
        RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

        // Build the payload
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", totalInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

        // 2. FIXED: Explicitly tell Java this is a Razorpay Order, not your Database
        // Order
        com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

        // Return the unique Razorpay Order ID (e.g., "order_EKwxw...")
        return razorpayOrder.get("id");
    }

    /**
     * Phase 2: Verify the payment signature and fulfill the order
     */
    @Transactional
    public boolean verifyAndFulfill(String email, String orderId, String paymentId, String signature,
            double actualAmount) {
        try {
            // Verify the signature to ensure the request actually came from Razorpay
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                // 3. FIXED: Just use "Order" since your database entity is imported at the top
                Order newOrder = Order.builder()
                        .userEmail(email)
                        .totalAmount(actualAmount)
                        .status("PAID")
                        .build();  

                orderRepository.save(newOrder);
                // Clear the User's Cart
                cartRepository.deleteByUserEmail(email);

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