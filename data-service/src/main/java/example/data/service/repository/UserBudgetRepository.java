package example.data.service.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import example.data.service.entity.User;

import java.util.Optional;

public interface UserBudgetRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
} 
