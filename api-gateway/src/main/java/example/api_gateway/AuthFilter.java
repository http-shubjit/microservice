package example.api_gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

        @Value("${app.jwt.secret}")
        private String jwtSecret;

        private Key signingKey;

        public AuthFilter() {
                super(Config.class);
        }

        /**
         * Convert secret string into secure signing key
         */
        @PostConstruct
        public void init() {
                this.signingKey = Keys.hmacShaKeyFor(
                                jwtSecret.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public GatewayFilter apply(Config config) {

                return (exchange, chain) -> {
 // Step 1: Get Authorization header
                         String authHeader = exchange.getRequest()
                                        .getHeaders()
                                        .getFirst(HttpHeaders.AUTHORIZATION);

                        // Step 2: Check if header exists
                        if (!StringUtils.hasText(authHeader)) {
                                return sendError(
                                                exchange,
                                                "Authorization header is missing",
                                                HttpStatus.UNAUTHORIZED);
                        }

                        // Step 3: Check Bearer token format
                        if (!authHeader.startsWith("Bearer ")) {
                                return sendError(
                                                exchange,
                                                "Invalid token format",
                                                HttpStatus.UNAUTHORIZED);
                        }

                        // Step 4: Extract token
                        String token = authHeader.substring(7);

                        try {

                                // Step 5: Validate token
                                Claims claims = Jwts.parserBuilder()
                                                .setSigningKey(signingKey)
                                                .build()
                                                .parseClaimsJws(token)
                                                .getBody();

                                // Step 6: Extract user data from token
                                String email = claims.get("email", String.class);
                                String role = claims.get("role", String.class);

                                // Step 7: Check role access
                                String requiredRole = config.getRequiredRole();

                                if (requiredRole != null && !requiredRole.isEmpty()) {

                                        if (!hasPermission(role, requiredRole)) {

                                                return sendError(
                                                                exchange,
                                                                "Access DenIED",
                                                                HttpStatus.FORBIDDEN);
                                        }
                                }

                                // Step 8: Add user info to request headers
                                ServerWebExchange modifiedExchange = exchange.mutate()
                                                .request(builder -> builder
                                                                .header("X-User-Email", email)
                                                                .header("X-User-Role", role))
                                                .build();

                                // Step 9: Forward request
                                return chain.filter(modifiedExchange);

                        } catch (JwtException e) {

                                // Invalid token / expired token / wrong signature
                                return sendError(
                                                exchange,
                                                "Invalid or Expired Token",
                                                HttpStatus.UNAUTHORIZED);
                        }
                };
        }

        /**
         * Role checking logic
         */
        private boolean hasPermission(String userRole, String requiredRole) {

                if (userRole == null) {
                        return false;
                }

                // SYSTEM_ADMINISTRATOR can access everything
                if (userRole.equalsIgnoreCase("SYSTEM_ADMINISTRATOR")) {
                        return true;
                }

                // USER can access USER endpoints
                if (userRole.equalsIgnoreCase(requiredRole)) {
                        return true;
                }

                return false;
        }

        /**
         * Send JSON error response
         */
        private Mono<Void> sendError(
                        ServerWebExchange exchange,
                        String message,
                        HttpStatus status) {

                var response = exchange.getResponse();

                response.setStatusCode(status);

                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                String body = """
                                {
                                    "error": "%s"
                                }
                                """.formatted(message);

                DataBuffer buffer = response.bufferFactory()
                                .wrap(body.getBytes(StandardCharsets.UTF_8));

                return response.writeWith(Mono.just(buffer));
        }

        /**
         * Configuration class
         */
        public static class Config {

                private String requiredRole;

                public String getRequiredRole() {
                        return requiredRole;
                }

                public void setRequiredRole(String requiredRole) {
                        this.requiredRole = requiredRole;
                }
        }
}