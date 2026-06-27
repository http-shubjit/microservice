package example.auth.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Default to USER if no role is provided
        User.Role assignedRole = User.Role.USER; // Default fallback

        String inputRole = request.getRole();

        // 1. Check if the input is actually provided and is not the Swagger placeholder
        // "string"
        if (inputRole != null && !inputRole.trim().isEmpty() && !inputRole.equalsIgnoreCase("string")) {
            try {
                assignedRole = User.Role.valueOf(inputRole.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                assignedRole = User.Role.USER; // Fallback if typo/invalid string is sent
            }
        } else {
          
        System.out.println("Swagger placeholder detected. Defaulting to USER.");
          assignedRole = User.Role.USER;
        }

        User user = User.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered → {} with role {}", saved.getEmail(), saved.getRole());

        return buildResponse(saved);
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        log.info("User logged in → {}", user.getEmail());
        return buildResponse(user);
    }

    public void logout(String token) {

        String jti = jwtUtil.extractJti(token);

        Date expiration = jwtUtil.extractExpiration(token);

        long remainingTime = expiration.getTime()
                - System.currentTimeMillis();

        if (remainingTime <= 0) {
            return;
        }

        tokenBlacklistService.blacklistToken(
                jti,
                Duration.ofMillis(remainingTime));

        log.info("Token revoked successfully");
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