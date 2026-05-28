package by.niruin.techprocess_service.model.technological_process;

import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.BlankType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateOperationRequest(
        @NotNull
        @Pattern(regexp = "^[0-9]{3,4}[а-я]$", message = "Неверный формат номера операции")
        String number,

        @NotNull
        @Pattern(regexp = "^[А-Яа-яёЁ0-9\\s-]+$", message = "Некорректное наименование операции")
        String name,

        List<@Pattern(regexp = "^[0-9]{5}$") String> workerCodes,

        @NotNull
        List<SafetyInstructionReference> safetyInstructionReferences,

        EquipmentReference equipmentReference,

        @Pattern(regexp = "^[1-6]$")
        String workerCategory,

        @NotNull
        BlankType blankType,

        Double weight,

        Integer area,
        @NotNull
        Boolean isOnlyForMan) {
}