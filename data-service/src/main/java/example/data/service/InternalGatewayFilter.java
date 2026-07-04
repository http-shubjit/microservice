package example.data.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalGatewayFilter extends OncePerRequestFilter {

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String secret = request.getHeader("X-Internal-Secret");

        if (!internalSecret.equals(secret)) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                        "error":"Access allowed only through API Gateway"
                    }
                    """);

            return;
        }

        filterChain.doFilter(request, response);
    }
}