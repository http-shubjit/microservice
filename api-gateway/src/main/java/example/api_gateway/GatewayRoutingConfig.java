package example.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // ROUTE 0 : AUTH SERVICE SWAGGER DOCS
                .route("auth-docs", r -> r.path("/auth/v3/api-docs")
                        .uri("lb://auth-server"))

                // ROUTE 1 : DATA SERVICE SWAGGER DOCS
                .route("data-docs", r -> r.path("/data/v3/api-docs")
                        .uri("lb://data-service"))

                // ROUTE 2 : AUTH SERVICE APIs
                .route("auth-server", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("authCircuitBreaker")
                                .setFallbackUri("forward:/fallback/auth-down")))
                        .uri("lb://auth-server"))

                // ROUTE 4 : EXCLUSIVE PUBLIC ENDPOINTS
                .route("data-public-endpoints", r -> r.path("/api/v1/public/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("dataCircuitBreaker")
                                .setFallbackUri("forward:/fallback/data-down")))
                        .uri("lb://data-service"))

                // ROUTE 5 : SECURE CATCH-ALL
                .route("data-shared-records", r -> r.path("/api/v1/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("dataCircuitBreaker")
                                .setFallbackUri("forward:/fallback/data-down")))
                        .uri("lb://data-service"))

                // ROUTE 6 : SANDBOX PAYMENT PAGE ROUTING
                .route("data-payment", r -> r.path("/payment.html")
                        .uri("lb://data-service"))

                .build();
    }
}
