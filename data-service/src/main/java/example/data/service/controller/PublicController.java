package example.data.service.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Information API", description = "Unauthenticated open endpoints.")
public class PublicController {

    @GetMapping("/dashboard")
    @Operation(summary = "View landing dashboard telemetry data")
    public ResponseEntity<String> viewDashboard() {
        return ResponseEntity.ok("Public Dashboard Data");
    }

    @GetMapping("/status")
    @Operation(summary = "Health check monitoring endpoint")
    public ResponseEntity<String> getSystemStatus() {
        return ResponseEntity.ok("System is up");
    }
}