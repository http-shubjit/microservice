package example.data.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartRepository;

    public CartItem addToCart(String email, String productId, String title, double price) {
        // Check if item already exists in this specific user's cart
        Optional<CartItem> existingItem = cartRepository.findByUserEmailAndProductId(email, productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + 1);
            return cartRepository.save(item);
        } else {
            CartItem newItem = new CartItem(null, email, productId, title, price, 1);
                        return cartRepository.save(newItem);
        }
    }

    public List<CartItem> getCartContents(String email) {
        return cartRepository.findByUserEmail(email);
    }
}