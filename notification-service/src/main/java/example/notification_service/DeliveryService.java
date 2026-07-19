// package main.java.example.notification_service;
// import com.resend.Resend;
// import com.resend.core.exception.ResendException;
// import com.resend.services.emails.model.CreateEmailOptions;
// import com.resend.services.emails.model.CreateEmailResponse;
// import jakarta.annotation.PostConstruct;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// @Service
// public class DeliveryService {

//     @Value("${resend.api.key}")
//     private String resendApiKey;

//     @Value("${resend.from.email}")
//     private String fromEmail;

//     private Resend resendClient;

//     // Initialize the Resend client once when Spring Boot starts
//     @PostConstruct
//     public void init() {
//         this.resendClient = new Resend(resendApiKey);
//     }

//     public void sendEmail(String toEmail, String userName) {
//         // Construct the HTML body
//         String htmlBody = String.format(
//                 "<h1>Welcome, %s!</h1><p>We are thrilled to have you here. Enjoy exploring!</p>",
//                 userName);

//         // Build the email request
//         CreateEmailOptions params = CreateEmailOptions.builder()
//                 .from(fromEmail)
//                 .to(toEmail)
//                 .subject("Welcome to our Platform!")
//                 .html(htmlBody)
//                 .build();

//         try {
//             // Execute the API call
//             CreateEmailResponse data = resendClient.emails().send(params);
//             System.out.println("Email sent successfully via Resend. ID: " + data.getId());
//         } catch (ResendException e) {
//             System.err.println("Failed to send email via Resend: " + e.getMessage());
//             // In a real app, you would throw a custom exception here to trigger the
//             // RabbitMQ retry
//             throw new RuntimeException("Email delivery failed", e);
//         }
//     }
// }