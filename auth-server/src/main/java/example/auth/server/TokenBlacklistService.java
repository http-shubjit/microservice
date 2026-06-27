package example.auth.server;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    private static final String PREFIX = "blacklist:";

    public void blacklistToken(
            String jti,
            Duration ttl) {

        redisTemplate.opsForValue().set(
                        PREFIX + jti,
                        "REVOKED",
                        ttl);
    }
}