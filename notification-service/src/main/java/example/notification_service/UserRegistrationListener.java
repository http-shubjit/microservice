// package main.java.example.notification_service;

// import com.example.notification.service.DeliveryService;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// @Component
// public class UserRegistrationListener {

//     @Autowired
//     private DeliveryService deliveryService;

//     @RabbitListener(queues = "notification_queue")
//     public void handleUserRegisteredEvent(UserEventPayload payload) {
//         System.out.println("Processing event for: " + payload.getName());

//         // This will now use Resend under the hood
//         deliveryService.sendEmail(payload.getEmail(), payload.getName());
//     }
// }