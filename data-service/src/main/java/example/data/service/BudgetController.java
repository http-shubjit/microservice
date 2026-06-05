package example.data.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Shared Budget API", description = "Endpoints accessible by both authenticated Users and Admins.")
public class BudgetController {

    @Autowired
    private UserBudgetRepository budgetRepository;
    @Autowired
    private RecommendationService recommendationService;

    @PostMapping("/submit/record")
    @Operation(summary = "Submit or Update budget metrics", description = "Saves financial details to database. Handles calculations dynamically based on permissions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully processed budget data and updated database."),
            @ApiResponse(responseCode = "400", description = "Invalid request payload supplied."),
            @ApiResponse(responseCode = "403", description = "Gateway authorization failure.")
    })
    public ResponseEntity<String> submitData(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String email,
            @Parameter(hidden = true) @RequestHeader("X-User-Role") String role,
            @RequestBody BudgetRequest request) {

        UserBudget budget;
        Optional<UserBudget> existingUser = budgetRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            budget = existingUser.get();
            budget.setMonthlyIncome(request.getMonthlyIncome());
            budget.setMonthlyExpense(request.getMonthlyExpense());
            budget.setRole(role);
        } else {
            budget = new UserBudget(null, email, role, request.getMonthlyIncome(), request.getMonthlyExpense());
        }

        budgetRepository.save(budget);

        double income = budget.getMonthlyIncome();
        double expense = budget.getMonthlyExpense();
        double balance = income - expense;

        if ("SYSTEM_ADMINISTRATOR".equalsIgnoreCase(role)) {
            String adminResponse = String.format(
                    "Admin Data processed for %s.\n" +
                            "Current Status: Income: $%.2f | Expenses: $%.2f | Net Balance: $%.2f",
                    email, income, expense, balance);
            return ResponseEntity.ok(adminResponse);
        }

        double idealMaxExpense = income * 0.70;
        String advice;

        if (balance < 0) {
            advice = String.format(
                    "Warning! You are overspending by $%.2f. You must cut down your expenses immediately.",
                    Math.abs(balance));
        } else if (expense > idealMaxExpense) {
            double potentialSavings = expense - idealMaxExpense;
            advice = String.format(
                    "Your account is positive, but you are spending %.1f%% of your income. Try to cut back by $%.2f to hit healthy savings goals.",
                    ((expense / income) * 100), potentialSavings);
        } else {
            advice = String.format(
                    "Great job! You are saving $%.2f this month. Keep maintaining this spending profile.", balance);
        }

        // --- NEW LOGIC: Fetch recommendations based on remaining balance ---
        String recommendationText = "";
        if (balance > 0) {
            List<String> affordableProducts = recommendationService.getAffordableProducts(balance);
            recommendationText = "\n\nReward yourself! Here are some items you can afford with your remaining balance:\n"
                    + String.join("\n", affordableProducts);
        }

        String userResponse = String.format(
                "Data processed for %s (Role: %s).\n" +
                        "Current Status: Income: $%.2f | Expenses: $%.2f\n" +
                        "Budget Recommendation: %s%s",
                email, role, income, expense, advice, recommendationText);

        return ResponseEntity.ok(userResponse);
    }
}