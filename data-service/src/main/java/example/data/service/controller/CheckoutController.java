package example.data.service.controller;

import example.data.service.dto.PaymentVerificationRequest;
import example.data.service.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    // Step 1 — Swagger: hit with JWT + X-User-Email, copy orderId from response
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateCheckout(
            @RequestHeader("X-User-Email") String email) throws Exception {

        Map<String, Object> orderDetails = checkoutService.createRazorpayOrderFromCart(email);

        return ResponseEntity.ok(Map.of(
                "razorpayOrderId", orderDetails.get("razorpayOrderId"),
                "amount", orderDetails.get("amountDisplay"),
                "productTitle", orderDetails.get("productTitle")));
    }

    // Step 2 — Called by payment.html after successful Razorpay payment
    // Verifies payment + saves shipping address + clears cart
    @PostMapping("/buy")
    public ResponseEntity<String> verifyPayment(
            @RequestHeader("X-User-Email") String email,
            @RequestBody PaymentVerificationRequest request) {

        boolean isSuccessful = checkoutService.verifyAndFulfill(email, request);

        if (isSuccessful) {
            return ResponseEntity.ok("Payment successful! Order confirmed and cart cleared.");
        } else {
            return ResponseEntity.status(400).body("Payment verification failed.");
        }
    }

    @PostMapping("/fail")
    public ResponseEntity<String> failPayment(@RequestParam String razorpayOrderId) {
        return ResponseEntity.ok("Order marked as failed.");
    }
}