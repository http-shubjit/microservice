package example.data.service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import example.data.service.dto.Product;
import example.data.service.dto.ProductResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final RestTemplate restTemplate;

    public RecommendationService() {
        this.restTemplate = new RestTemplate();
    }

    public List<Product> getAffordableProducts(double budgetCapacity) {
        // 1. Return empty list if budget capacity is invalid
        if (budgetCapacity <= 0) {
            return Collections.emptyList();
        }

        String apiUrl = "https://dummyjson.com/products?limit=5";

        try {
            ProductResponse response = restTemplate.getForObject(apiUrl, ProductResponse.class);

            if (response != null && response.getProducts() != null) {
                // 2. Stream, filter by price, and return full Product objects
                return response.getProducts().stream()
                        .filter(p -> p.getPrice() <= budgetCapacity)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            System.err.println("Failed to fetch products: " + e.getMessage());
            return Collections.emptyList();
        }

        // 3. Fallback if response or products array was completely null
        return Collections.emptyList();
    }
}