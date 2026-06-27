package example.data.service.dto;

import java.util.List;
import lombok.Data;

@Data

public class ProductResponse {
    private List<Product> products;
    private int total;
    private int skip;
    private int limit;

}