package example.data.service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private double totalAmount;

    @Column(nullable = false)
    private String status; // Now can be: "PENDING", "PAID", or "FAILED"

    @Column(unique = true)
    private String razorpayOrderId; // Added to map DB record to Razorpay's system

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}