package example.data.service.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload required to submit or update monthly financial tracking data")
public class BudgetRequest {

    @Schema(description = "Total monthly income of the user", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private double monthlyIncome;

    @Schema(description = "Total current monthly expenditures of the user", example = "3200.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private double monthlyExpense;
}