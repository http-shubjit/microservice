package example.api_gateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayFallbackController {

    @GetMapping("/fallback/auth-down")
    public String authServiceFallback() {
        return "Authentication Service is currently unavailable. Please try again shortly.";
    }

    @GetMapping("/fallback/data-down")
    public String dataServiceFallback() {
        return "Data Service is experiencing technical difficulties or high traffic. Fallback triggered.";
    }
}