package example.data.service.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    private String role;
    private double monthlyIncome;
    private double monthlyExpense;
   
}