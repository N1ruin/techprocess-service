package by.niruin.techprocess_service.model.technological_process;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTechprocessRequest(
        @Pattern(regexp = "^[А-Я0-9.-]+-\\d{7}(-[A-Z0-9А-Я]+)?$")
        String partNumber,
        @NotNull
        @Pattern(regexp = "^([А-Яа-яёЁ0-9-\\s]+)?$")
        String partName,
        @NotNull
        @Pattern(regexp = "^[0-9]{5}$")
        String archiveNumber,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$")
        String reviewerFirstName,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$")
        String reviewerLastName,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$")
        String reviewerFatherName,
        @NotNull
        @Pattern(regexp = "^[0-9]{3}$")
        String workshopCode,
        @NotNull
        @Pattern(regexp = "^(ASSEMBLY|GENERAL_PURPOSE|TESTS)$")
        String workType,
        @NotNull
        @Pattern(regexp = "^(SINGLE|TYPICAL|GROUP)$")
        String organizationType,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁ ]+$")
        String workName,
        String routeCardNote) {
}
