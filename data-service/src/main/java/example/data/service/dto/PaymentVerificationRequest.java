package example.data.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaymentVerificationRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private double amountPaid;
    private List<Long> cartItemIds; 
}