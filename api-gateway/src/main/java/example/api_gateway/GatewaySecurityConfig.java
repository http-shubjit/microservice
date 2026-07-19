package example.api_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

import java.util.List;

import javax.crypto.spec.SecretKeySpec;



@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

        private final TokenBlacklistService tokenBlacklistService;
        private final ReactiveJwtDecoder jwtDecoder;

        public GatewaySecurityConfig(TokenBlacklistService tokenBlacklistService,
                        @Value("${app.jwt.secret}") String jwtSecret) {
                this.tokenBlacklistService = tokenBlacklistService;

                // Built ONCE at startup, not on every request
                SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
                this.jwtDecoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                                .macAlgorithm(MacAlgorithm.HS256)
                                .build();
        }

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

                http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**")
                                                .permitAll()
                                                .pathMatchers("/v3/api-docs/**", "/*/v3/api-docs/**", "/*.html")
                                                .permitAll()
                                                .pathMatchers("/api/v1/auth/**", "/api/v1/public/**", "/fallback/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtDecoder(token -> decodeWithBlacklistCheck(token))));

                return http.build();
        }

        private Mono<Jwt> decodeWithBlacklistCheck(String token) {
                return jwtDecoder.decode(token)
                                .flatMap(jwt -> tokenBlacklistService.isBlacklisted(jwt.getId())
                                                .flatMap(blacklisted -> {
                                                        if (Boolean.TRUE.equals(blacklisted)) {
                                                                return Mono.<Jwt>error(new JwtException(
                                                                                "Token has been revoked"));
                                                        }
                                                        return Mono.just(jwt);
                                                }));
        }

        private CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:8080", "http://localhost:3000"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "Accept",
                                "X-Requested-With"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}