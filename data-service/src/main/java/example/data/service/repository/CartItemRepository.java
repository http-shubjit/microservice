package example.data.service.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import example.data.service.entity.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserEmail(String userEmail);
    Optional<CartItem> findByUserEmailAndProductId(String userEmail, int productId);
    void deleteByUserEmail(String userEmail);
    void deleteByUserEmailAndProductId(String email, int productId);
}