package example.data.service.dto;
import lombok.Data;

@Data
public class AddToCartRequest {
    private int productId;
    private String productTitle;
    private double price;
}