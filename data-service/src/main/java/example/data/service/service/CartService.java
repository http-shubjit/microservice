package example.data.service.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import example.data.service.entity.CartItem;
import example.data.service.repository.CartItemRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartRepository;

    public CartItem addToCart(String email, int productId, String title, double price, int quantity) {
        Optional<CartItem> existingItem = cartRepository.findByUserEmailAndProductId(email, productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity); 
            return cartRepository.save(item);
        } else {
            CartItem newItem = new CartItem(null, email, productId, title, price, quantity);
            return cartRepository.save(newItem);
        }
    }

    public List<CartItem> getCartContents(String email) {
        return cartRepository.findByUserEmail(email);
    }


   public void removeFromCart(String email, int productId) {
        Optional<CartItem> existingItem = cartRepository.findByUserEmailAndProductId(email, productId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartRepository.save(item);
            } else {
                cartRepository.delete(item);
            }
        }
    } 
}