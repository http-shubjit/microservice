package example.data.service.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import example.data.service.dto.AddToCartRequest;
import example.data.service.dto.Product;
import example.data.service.entity.CartItem;
import example.data.service.service.CartService;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<String> addItemToCart(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String email,
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