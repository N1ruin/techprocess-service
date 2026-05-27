package by.niruin.techprocess_service.model.technological_process;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTechprocessRequest(
        @Pattern(regexp = "^[А-Я0-9.-]+-\\d{7}(-[A-Z0-9А-Я]+)?$", message = "Неверный формат номера детали/узла")
        String partNumber,
        @NotNull
        @Pattern(regexp = "^[А-Яа-яёЁ0-9-\\s]+$")
        String partName,
        @NotNull
        @Pattern(regexp = "^[0-9]{5}$", message = "Архивный номер должен состоять из 5 цифр")
        String archiveNumber,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$", message = "Неверный формат фамилии проверяющего")
        String reviewerFirstName,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$", message = "Неверный формат фамилии разработчика")
        String reviewerLastName,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁa-zA-Z-]+$", message = "Неверный формат фамилии разработчика")
        String reviewerFatherName,
        @NotNull
        @Pattern(regexp = "^[0-9]{3}$", message = "Код цеха должен состоять из трех цифр")
        String workshopCode,
        @NotNull
        @Pattern(regexp = "^(ASSEMBLY|GENERAL_PURPOSE|TESTS)$", message = "Некорректный вид техпроцесса по выполняемым работам")
        String workType,
        @NotNull
        @Pattern(regexp = "^(SINGLE|TYPICAL|GROUP)$", message = "Некорректный тип технологического процесса")
        String organizationType,
        @NotNull
        @Pattern(regexp = "^[а-яА-ЯёЁ ]+$")
        String workName) {
}
