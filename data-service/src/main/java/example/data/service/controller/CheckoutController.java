package example.data.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import example.data.service.dto.CheckoutInitiateRequest;
import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.entity.CartItem;
import example.data.service.service.CheckoutService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
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
                request.getRazorpaySignature()
                    );

        if (isSuccessful) {
            return ResponseEntity.ok("Payment successful!");
        } else {
            return ResponseEntity.status(400).body("Payment verification failed.");
        }
    }

    /**
     * NEW ENDPOINT: Called by frontend if user closes popup or payment fails
     */
    @PostMapping("/fail")
    public ResponseEntity<String> failPayment(@RequestParam String razorpayOrderId) {
        checkoutService.markOrderAsFailed(razorpayOrderId);
        return ResponseEntity.ok("Order status updated to FAILED.");
    }
}