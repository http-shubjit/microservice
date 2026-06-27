package example.data.service.controller;

import example.data.service.entity.CartItem; // Assuming this is your CartItem entity package
import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCheckout(
            @RequestHeader("X-User-Email") String email,
            @RequestBody List<CartItem> request) {
        try {
            String razorpayOrderId = checkoutService.createRazorpayOrder(email, request);
            return ResponseEntity.ok(Map.of("razorpayOrderId", razorpayOrderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate checkout: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestHeader("X-User-Email") String email,
            @RequestBody PaymentVerificationRequest request) {

        boolean isSuccessful = checkoutService.verifyAndFulfill(
                email,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (isSuccessful) {
            return ResponseEntity.ok("Payment successful and cart cleared!");
        } else {
            return ResponseEntity.status(400).body("Payment verification failed.");
        }
    }
}