package example.data.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final RestTemplate restTemplate;

    public RecommendationService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetches products from DummyJSON and returns the top 3 affordable items.
     * 
     * @param budgetCapacity The maximum price the user can afford.
     */
    public List<String> getAffordableProducts(double budgetCapacity) {
        if (budgetCapacity <= 0) {
            return List.of("No available budget for shopping this month.");
        }

        String apiUrl = "https://dummyjson.com/products?limit=50";

        try {
            // 1. Fetch data from the API
            DummyJsonResponse response = restTemplate.getForObject(apiUrl, DummyJsonResponse.class);

            if (response != null && response.getProducts() != null) {
                // 2. Filter products by the user's budget capacity
                return response.getProducts().stream()
                        .filter(product -> product.getPrice() <= budgetCapacity)
                        // Sort so the nicest items they can afford appear first
                        .sorted((p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()))
                        .limit(3) // Only show the top 3 recommendations
                        .map(product -> String.format("- %s ($%.2f)", product.getTitle(), product.getPrice()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch products: " + e.getMessage());
            return List.of("Product recommendations are currently unavailable.");
        }

        return List.of("No products found within your budget.");
    }
}