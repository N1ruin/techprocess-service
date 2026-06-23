package by.niruin.techprocess_service.model.file;

import jakarta.validation.constraints.NotNull;

public record UploadFileResponse(@NotNull String fileName) {
}
