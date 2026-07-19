package example.notification_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeMessage(String rawJsonMessage) {
        try {
        Map<String, String> data = objectMapper.readValue(rawJsonMessage, Map.class);
            String email = data.get("email");
            String fullName = data.get("fullname");

            if (email == null || email.isBlank()) {
                log.error("Registration event missing email: {}", rawJsonMessage);
                return;
            }

            log.info("Sending welcome email to {} ({})", email, fullName);

            String subject = "Welcome to Our Platform – Registration Confirmed!";
            String body = "Dear " + fullName + ",\n\n"
                    + "Thank you for registering an account with us. We are thrilled to have you on board!\n\n"
                    + "Your account has been successfully created and is now ready for use.\n\n"
                    + "Best regards,\nRaka Biswal\nPlatform Administrator";

            emailService.sendSimpleMail(new EmailDetails(email, body, subject));
        } catch (Exception e) {
            log.error("Failed to process registration message: {}", rawJsonMessage, e);
        }
    }
}