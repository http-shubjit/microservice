package example.auth.server;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = User.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(resolveRole(request.getRole()))
                .build();

        User saved = userRepository.save(user);
        log.info("User registered -> {} with role {}", saved.getEmail(), saved.getRole());

        publishUserRegistered(saved);

        return buildResponse(saved);
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        log.info("User logged in -> {}", user.getEmail());
        return buildResponse(user);
    }

    public void logout(String token) {
        String jti = jwtUtil.extractJti(token);
        Date expiration = jwtUtil.extractExpiration(token);

        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        if (remainingTime <= 0) {
            return;
        }

        tokenBlacklistService.blacklistToken(jti, Duration.ofMillis(remainingTime));
        log.info("Token revoked successfully");
    }

    // Only send what the notification service needs - never the entity itself
    // (it carries the password hash and can have lazy fields that break Jackson).
    private void publishUserRegistered(User user) {
        try {
            Map<String, String> event = Map.of(
                    "email", user.getEmail(),
                    "fullname", user.getFullname() == null ? "" : user.getFullname());

            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, payload);
            log.info("Published registration event for {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish registration event for {}", user.getEmail(), e);
        }
    }

    private User.Role resolveRole(String inputRole) {
        if (inputRole == null || inputRole.isBlank() || inputRole.equalsIgnoreCase("string")) {
            return User.Role.USER;
        }
        try {
            return User.Role.valueOf(inputRole.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return User.Role.USER;
        }
    }

    private Map<String, Object> buildResponse(User user) {
        return Map.of(
                "token", jwtUtil.generateToken(user),
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullname(),
                "role", user.getRole().name());
    }
}