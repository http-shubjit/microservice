package example.auth.server;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Login and registration endpoints")
public class AuthController {

        private final AuthService authService;
    
        public AuthController(AuthService authService) {
        
            this.authService = authService;
        }

    @SecurityRequirements
    @Operation(summary = "Register a new user", description = "Creates account and returns JWT token")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse<?>> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(AuthResponse.success(
                            authService.register(request)));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(AuthResponse.error(e.getMessage()));
        }
    }

    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse<?>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(
                    AuthResponse.success(authService.login(request)));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Logout", description = "Invalidates the JWT token")
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse<?>> logout(
                    @RequestHeader(value = "Authorization", required = false) String authHeader) {

            if (authHeader == null || authHeader.isBlank()) {
                    return ResponseEntity.badRequest()
                                    .body(AuthResponse.error("Missing Authorization Header"));
            }

           
            String token = authHeader.startsWith("Bearer ")
                            ? authHeader.substring(7)
                            : authHeader;

            authService.logout(token);

            return ResponseEntity.ok(
                            AuthResponse.success("Logout successful"));
    }


    @SecurityRequirements
    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<AuthResponse<?>> health() {
        return ResponseEntity.ok(
                AuthResponse.success("Auth service is up and running"));
    }
}