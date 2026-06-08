package by.niruin.techprocess_service.model.technological_process;

import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record UpdateOperationRequest(
        @NotNull
        @Pattern(regexp = "^[0-9]{3,4}[а-я]?$")
        String number,

        @NotNull
        @Pattern(regexp = "^[А-Яа-яёЁ0-9\\s-]+$")
        String name,

        List<@Pattern(regexp = "^[0-9]{5}$") String> workerCodes,

        @NotNull
        List<SafetyInstructionReference> safetyInstructionReferences,

        List<PartReference> partReferences,

        EquipmentReference equipmentReference,

        @Pattern(regexp = "^[1-6]$")
        String workerCategory,

        @NotNull
        BlankType blankType,

        Double weight,

        Integer area,
        @NotNull
        Boolean isOnlyForMan,
        @NotNull
        OperationType operationType,

        @NotNull
        List<MaterialReference> materialReferences,

        @NotNull
        List<TechnologicalTransition> transitions,

        List<SketchCard> sketchCards,

        List<MultipartFile> newSketchFiles) {
}
