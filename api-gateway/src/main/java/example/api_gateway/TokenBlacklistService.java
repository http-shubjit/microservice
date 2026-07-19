package example.api_gateway;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenBlacklistService {

    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private static final String PREFIX = "blacklist:";

    public TokenBlacklistService(ReactiveStringRedisTemplate reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    /**
     * Checks if a token ID (jti) exists in Redis.
     * Returns a Mono (Reactive Boolean) so it doesn't block the server thread.
     */
    public Mono<Boolean> isBlacklisted(String jti) {
        if (jti == null) {
            return Mono.just(false);
        }
        return reactiveRedisTemplate.hasKey(PREFIX + jti);
    }
}