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

        private final TokenBlacklistService tokenBlacklistService;

        public AuthFilter(TokenBlacklistService tokenBlacklistService) {

                super(Config.class);

                this.tokenBlacklistService = tokenBlacklistService;
        }
        @PostConstruct
        public void init() {

                this.signingKey = Keys.hmacShaKeyFor(
                                jwtSecret.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public GatewayFilter apply(Config config) {

                return (exchange, chain) -> {

                        String token = extractToken(exchange);

                        if (token == null) {

                                return sendError(
                                                exchange,
                                                "Authorization header is missing or invalid",
                                                HttpStatus.UNAUTHORIZED);
                        }

                        try {

                                Claims claims = validateAndExtractClaims(token);

                                String email = claims.get("email", String.class);

                                String role = claims.get("role", String.class);

                                String jti = claims.getId();

                                // ==================================================
                                // BLACKLIST CHECK
                                // ==================================================

                                if (tokenBlacklistService.isBlacklisted(jti)) {

                                        return sendError(
                                                        exchange,
                                                        "Token has been revoked",
                                                        HttpStatus.UNAUTHORIZED);
                                }

                                // ==================================================
                                // ROLE CHECK
                                // ==================================================

                                String requiredRole = config.getRequiredRole();

                                if (StringUtils.hasText(requiredRole)
                                                && !hasPermission(role, requiredRole)) {

                                        return sendError(
                                                        exchange,
                                                        "Access Denied",
                                                        HttpStatus.FORBIDDEN);
                                }

                                // ==================================================
                                // FORWARD USER INFO
                                // ==================================================

                                ServerWebExchange modifiedExchange = exchange.mutate()
                                                .request(builder -> builder
                                                                .header("X-User-Email", email)
                                                                .header("X-User-Role", role))
                                                .build();

                                return chain.filter(modifiedExchange);

                        } catch (JwtException ex) {

                                return sendError(
                                                exchange,
                                                "Invalid or Expired Token",
                                                HttpStatus.UNAUTHORIZED);
                        }
                };
        }

        private String extractToken(
                        ServerWebExchange exchange) {

                String authHeader = exchange.getRequest()
                                .getHeaders()
                                .getFirst(HttpHeaders.AUTHORIZATION);

                if (!StringUtils.hasText(authHeader)) {
                        return null;
                }

                if (!authHeader.startsWith("Bearer ")) {
                        return null;
                }

                return authHeader.substring(7);
        }

        private Claims validateAndExtractClaims(
                        String token) {

                return Jwts.parserBuilder()
                                .setSigningKey(signingKey)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();
        }

        private boolean hasPermission(
                        String userRole,
                        String requiredRole) {

                if (userRole == null) {
                        return false;
                }

                if ("SYSTEM_ADMINISTRATOR"
                                .equalsIgnoreCase(userRole)) {

                        return true;
                }

                return userRole.equalsIgnoreCase(requiredRole);
        }

        private Mono<Void> sendError(
                        ServerWebExchange exchange,
                        String message,
                        HttpStatus status) {

                var response = exchange.getResponse();

                response.setStatusCode(status);

                response.getHeaders()
                                .setContentType(MediaType.APPLICATION_JSON);

                String body = """
                                {
                                    "error": "%s"
                                }
                                """.formatted(message);

                DataBuffer buffer = response.bufferFactory()
                                .wrap(body.getBytes(StandardCharsets.UTF_8));

                return response.writeWith(
                                Mono.just(buffer));
        }

        public static class Config {

                private String requiredRole;

                public String getRequiredRole() {
                        return requiredRole;
                }

                public void setRequiredRole(
                                String requiredRole) {

                        this.requiredRole = requiredRole;
                }
        }
}