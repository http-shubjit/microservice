package example.data.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder; // Import this!
import lombok.Data;
import lombok.NoArgsConstructor;
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

    private String userEmail;
    private double totalAmount;
    private String status;

    @Builder.Default // Tells Lombok to keep this default value when building
    private LocalDateTime createdAt = LocalDateTime.now();
}