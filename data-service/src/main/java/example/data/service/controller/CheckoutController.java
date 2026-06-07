package example.data.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import example.data.service.dto.CheckoutInitiateRequest;
import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.service.CheckoutService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    /**
     * 1. Initiates the checkout. Accepts selected item IDs and returns the Order
     * ID.
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCheckout(
            @RequestHeader("X-User-Email") String email,
            @RequestBody CheckoutInitiateRequest request) { // Added RequestBody
        try {
            // Pass the selected IDs to the service
            String razorpayOrderId = checkoutService.createRazorpayOrder(email, request.getCartItemIds());
            return ResponseEntity.ok(Map.of("razorpayOrderId", razorpayOrderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate checkout: " + e.getMessage());
        }
    }

    /**
     * 2. Verifies the payment and deletes the purchased items.
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestHeader("X-User-Email") String email,
            @RequestBody PaymentVerificationRequest request) {

        boolean isSuccessful = checkoutService.verifyAndFulfill(
                email,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                request.getAmountPaid(),
                request.getCartItemIds() // Pass the IDs to delete
        );

        if (isSuccessful) {
            return ResponseEntity.ok("Payment successful! Your budget has been updated.");
        } else {
            return ResponseEntity.status(400).body("Payment signature verification failed. Potential fraud detected.");
        }
    }
}