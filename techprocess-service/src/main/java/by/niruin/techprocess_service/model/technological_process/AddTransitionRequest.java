package by.niruin.techprocess_service.model.technological_process;

import by.niruin.techprocess_service.domain.EquipmentReference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record AddTransitionRequest(
        @NotNull
        @Pattern(regexp = "^[0-9]+$")
        Integer number,
        @NotNull
        String content,
        List<EquipmentReference> equipmentReferences) {
}
