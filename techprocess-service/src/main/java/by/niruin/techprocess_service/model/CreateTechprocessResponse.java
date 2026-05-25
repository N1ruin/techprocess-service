package by.niruin.techprocess_service.model;

import java.time.Instant;

public record CreateTechprocessResponse(String id,
                                        String name,
                                        String partNumber,
                                        String archiveNumber,
                                        String workshopCode,
                                        String type,
                                        String status,
                                        Integer revision,
                                        String workType,
                                        Instant createdDate,
                                        Instant updatedDate) {
}
