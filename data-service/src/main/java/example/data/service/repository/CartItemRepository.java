package example.data.service.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import example.data.service.entity.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 1. Fetch all items in a specific user's cart (for the View Cart page)
    List<CartItem> findByUserEmail(String userEmail);

    // 2. Check if a user already has a specific product in their cart (to increase
    // quantity instead of creating a duplicate)
    Optional<CartItem> findByUserEmailAndProductId(String userEmail, String productId);

    // 3. Clear the user's cart completely (crucial to run after a successful
    // checkout)
    void deleteByUserEmail(String userEmail);
}