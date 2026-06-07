package example.data.service.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.service.CheckoutService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    /**
     * 1. Initiates the checkout. Returns the Order ID to the frontend.
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCheckout(@RequestHeader("X-User-Email") String email) {
        try {
            String razorpayOrderId = checkoutService.createRazorpayOrder(email);
            return ResponseEntity.ok(Map.of("razorpayOrderId", razorpayOrderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initiate checkout: " + e.getMessage());
        }
    }

    /**
     * 2. The frontend sends the success tokens here after the popup closes.
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
                request.getAmountPaid());

        if (isSuccessful) {
            return ResponseEntity.ok("Payment successful! Your budget has been updated.");
        } else {
            return ResponseEntity.status(400).body("Payment signature verification failed. Potential fraud detected.");
        }
    }
}