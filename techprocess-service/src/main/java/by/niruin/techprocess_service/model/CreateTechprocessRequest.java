package by.niruin.techprocess_service.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTechprocessRequest(
        @NotNull
        @Pattern(regexp = "^[А-Яа-я0-9\\s]+$")
        String name,
        @Pattern(regexp = "^[А-Я0-9.-]+-\\d{7}(-[A-Z0-9А-Я]+)?$", message = "Неверный формат номера детали/узла")
        String partNumber,
        @NotNull
        @Pattern(regexp = "^[0-9]{5}$", message = "Архивный номер должен состоять из 5 цифр")
        String archiveNumber,
        @NotNull
        @Pattern(regexp = "^[0-9]{3}$", message = "Код цеха должен состоять из трех цифр")
        String workshopCode,
        @NotNull
        @Pattern(regexp = "^(SINGLE|TYPICAL|GROUP)$", message = "Некорректный тип технологического процесса")
        String type) {
}
