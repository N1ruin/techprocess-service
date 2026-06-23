package by.niruin.techprocess_service.model.technological_process;

import jakarta.validation.constraints.NotNull;

public record AddCommentRequest(@NotNull String content) {
}
