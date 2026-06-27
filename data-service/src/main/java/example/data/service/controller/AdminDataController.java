package example.data.service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import example.data.service.entity.User;
import example.data.service.repository.UserBudgetRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin System API", description = "Privileged endpoints restricted to SYSTEM_ADMINISTRATOR role.")
public class AdminDataController {

    @Autowired
    private UserBudgetRepository budgetRepository;

    @GetMapping("/system-config/properties")
    @Operation(summary = "Get system core properties", description = "Fetches environment configuration values.")
    public ResponseEntity<String> getSystemProperties(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String adminEmail,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok("System settings accessed by Admin: " + adminEmail + " (Role: " + role + ")");
    }

    @GetMapping("/users/all")
    @Operation(summary = "Fetch total user details", description = "Retrieves complete budget profiles of all registered users from H2.")
    public ResponseEntity<List<User>> getAllUserDetails() {
        List<User> allUsers = budgetRepository.findAll();
        return ResponseEntity.ok(allUsers);
    }
    
}