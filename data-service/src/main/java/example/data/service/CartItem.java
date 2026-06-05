package example.data.service;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Data // Lombok: Generates Getters, Setters, toString, equals, and hashCode
      // automatically
@NoArgsConstructor // Lombok: Generates the empty constructor required by JPA
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_title", nullable = false)
    private String productTitle;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}