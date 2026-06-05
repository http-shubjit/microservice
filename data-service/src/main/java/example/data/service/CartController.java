package example.data.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<String> addItemToCart(
            @RequestHeader("X-User-Email") String email,
            @RequestBody AddToCartRequest payload) {

        cartService.addToCart(
                email,
                payload.getProductId(),
                payload.getProductTitle(),
                payload.getPrice());

        return ResponseEntity.ok("Product successfully added to your cart!");
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> viewMyCart(@RequestHeader("X-User-Email") String email) {
        List<CartItem> cart = cartService.getCartContents(email);
        return ResponseEntity.ok(cart);
    }
}