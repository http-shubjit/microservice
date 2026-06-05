package example.data.service;
import lombok.Data;

@Data
public class AddToCartRequest {
    private String productId;
    private String productTitle;
    private double price;
}