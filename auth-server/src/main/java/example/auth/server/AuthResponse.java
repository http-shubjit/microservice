package example.auth.server;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse<T> {

    private String status;
    private T data;
    private String error;
    private LocalDateTime timestamp;

    public static <T> AuthResponse<T> success(T data) {
        return AuthResponse.<T>builder()
                .status("success")
                .data(data)
                .error(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> AuthResponse<T> error(String message) {
        return AuthResponse.<T>builder()
                .status("error")
                .data(null)
                .error(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}