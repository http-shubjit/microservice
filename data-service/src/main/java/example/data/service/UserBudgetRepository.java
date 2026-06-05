package example.data.service;



import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserBudgetRepository extends JpaRepository<UserBudget, Long> {
    Optional<UserBudget> findByEmail(String email);
} 
