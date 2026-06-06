package example.data.service.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import example.data.service.entity.UserBudget;

import java.util.Optional;

public interface UserBudgetRepository extends JpaRepository<UserBudget, Long> {
    Optional<UserBudget> findByEmail(String email);
} 
