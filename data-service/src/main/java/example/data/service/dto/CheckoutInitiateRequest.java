package example.data.service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CheckoutInitiateRequest {
    private List<Long> cartItemIds;
}