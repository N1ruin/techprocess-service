package by.niruin.techprocess_service.model.technological_process;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;

import java.time.Instant;

public record CreateTechprocessResponse(String id,
                                        String partNumber,
                                        String partName,
                                        String archiveNumber,
                                        String developerFirstName,
                                        String developerLastName,
                                        String developerFatherName,
                                        String reviewerFirstName,
                                        String reviewerLastName,
                                        String reviewerFatherName,
                                        String workshopCode,
                                        TechnologicalProcessOrganizationType organizationType,
                                        TechnologicalProcessStatus status,
                                        TechnologicalProcessWorkType workType,
                                        String routeCardNote,
                                        String workName,
                                        Integer revision,
                                        String fullNumber,
                                        Instant createdDate,
                                        Instant updatedDate,
                                        Instant reviewerApprovedDate) {
}
