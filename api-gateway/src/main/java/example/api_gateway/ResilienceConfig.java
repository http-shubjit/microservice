package example.api_gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // 1. Define the rules for the Data Service
        CircuitBreakerConfig dataConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreExceptions(ResponseStatusException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        // 2. Register the instances by their exact string names
        registry.circuitBreaker("dataCircuitBreaker", dataConfig);
        registry.circuitBreaker("authCircuitBreaker", dataConfig); // Reusing config or making a new one

        return registry;
    }
}
