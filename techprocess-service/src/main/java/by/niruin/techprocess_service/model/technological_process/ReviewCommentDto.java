package by.niruin.techprocess_service.model.technological_process;

import java.time.Instant;
import java.util.UUID;

public record ReviewCommentDto(UUID uuid,
                               String content,
                               Instant createdDate) {
}
