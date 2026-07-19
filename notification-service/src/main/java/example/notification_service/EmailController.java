package example.notification_service;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;

    // Send simple email
    @PostMapping("/sendMail")
    public String sendMail(
            @RequestBody EmailDetails details) {

        return emailService.sendSimpleMail(details);
    }

   
}