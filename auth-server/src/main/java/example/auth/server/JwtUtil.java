package example.auth.server;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration.ms}")
    private long expirationMs;

    public String generateToken(User user) {

        return Jwts.builder().
        claim("email", user.getEmail()).
        claim("role", user.getRole()).
        setIssuedAt(new Date(System.currentTimeMillis())).
        setExpiration(new Date(System.currentTimeMillis()+expirationMs)).
        signWith(getSigningKey(), SignatureAlgorithm.HS256).
        compact();

    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));
    }
}