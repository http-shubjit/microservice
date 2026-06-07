package example.auth.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        // Default to GUEST if no role is provided
        User.Role assignedRole = User.Role.GUEST;
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            try {
                assignedRole = User.Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role specified");
            }
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

    private Map<String, Object> buildResponse(User user) {
        return Map.of(
                "token", jwtUtil.generateToken(user),
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullname(),
                "role", user.getRole().name() 
        );
    }
}