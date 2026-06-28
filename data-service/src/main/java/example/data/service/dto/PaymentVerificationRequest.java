package example.data.service.dto;

import lombok.Data;

@Data
public class PaymentVerificationRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // Shipping details — saved after successful payment
    private String fullName;
    private String phone;
    private String address;
    private String city;
    private String pincode;
}