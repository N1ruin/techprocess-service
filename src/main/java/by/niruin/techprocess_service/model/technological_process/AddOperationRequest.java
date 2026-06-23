package by.niruin.techprocess_service.model.technological_process;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record AddOperationRequest(
        @NotNull
        @Pattern(regexp = "^[0-9]{3,4}[а-я]?$")
        String number,

        @NotNull
        @Pattern(regexp = "^[А-Яа-яёЁ0-9\\s-]+$")
        String name,

        List<@Pattern(regexp = "^[0-9]{5}$") String> workerCodes,

        @NotNull
        List<SafetyInstructionDto> safetyInstructions,

        List<PartDto> parts,

        List<MaterialDto> materials,

        EquipmentDto equipment,

        @Pattern(regexp = "^[1-6]$")
        String workerCategory,

        @NotNull
        @Pattern(regexp = "^(ROUTE_BLANK_TITLE" +
                          "|ROUTE_BLANK_CONTINUATION" +
                          "|SKETCH_BLANK_TITLE_A4" +
                          "|SKETCH_BLANK_TITLE_A3" +
                          "|SKETCH_BLANK_CONTINUATION_A4" +
                          "|SKETCH_BLANK_CONTINUATION_A3" +
                          "|OPERATION_BLANK_TITLE" +
                          "|OPERATION_BLANK_CONTINUATION" +
                          "|CONTROL_BLANK_TITLE" +
                          "|CONTROL_BLANK_CONTINUATION)$")
        String  blankType,

        Double weight,

        Integer area,
        @NotNull
        Boolean isOnlyForMan,
        @NotNull
        @Pattern(regexp = "^(ASSEMBLY|CONTROL)$")
        String operationType) {
}