package example.auth.server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wire-level DTO for the "user registered" event.
 *
 * This is the ONLY thing that should ever be published to RabbitMQ for this
 * event — never the JPA `User` entity itself. Reasons:
 * 1. Security: the entity carries the bcrypt password hash; this DTO doesn't.
 * 2. Stability: entity fields/relations can change or lazy-load and break
 * serialization; this DTO is a deliberate, stable contract between
 * auth-server and notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
}