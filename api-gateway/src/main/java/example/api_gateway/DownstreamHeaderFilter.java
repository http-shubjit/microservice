package example.api_gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class DownstreamHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(ctx -> ctx.getAuthentication() != null
                        && ctx.getAuthentication().getPrincipal() instanceof Jwt)
                .map(ctx -> (Jwt) ctx.getAuthentication().getPrincipal())
                .flatMap(jwt -> {
                    String email = jwt.getSubject();

                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .headers(httpHeaders -> {
                                // Strip any client-supplied value first so it can't be spoofed,
                                // then set the trusted value from the verified JWT.
                                httpHeaders.remove("X-User-Email");
                                httpHeaders.remove("Authorization");
                                httpHeaders.set("X-User-Email", email);
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1;
    }
}