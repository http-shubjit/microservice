package example.auth.server;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

   
    @NotBlank(message = "Full name is required")
    private String fullname;
    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
@Schema(example = "") 
    private String role;}