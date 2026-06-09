package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessResponse;
import by.niruin.techprocess_service.model.event.technological_process.*;
import by.niruin.techprocess_service.model.technological_process.TechnologicalProcessDto;
import by.niruin.techprocess_service.model.technological_process.UpdateTechprocessRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = EquipmentMapper.class)
public interface TechnologicalProcessMapper {
    TechnologicalProcess toTechnologicalProcess(CreateTechprocessRequest request);

    TechnologicalProcess toTechnologicalProcess(UpdateTechprocessRequest request);

    CreateTechprocessResponse toResponse(TechnologicalProcess technologicalProcess);

    TechnologicalProcessCreatedEvent toCreatedEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessCancelledEvent toCancelledEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessUpdatedEvent toUpdatedEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessSentToReviewEvent toSentToReviewEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessApprovedEvent toApprovedEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessReturnedAfterReviewEvent toReturnedAfterReviewEvent(TechnologicalProcess technologicalProcess);

    TechnologicalProcessDto toDto(TechnologicalProcess techprocess);
}
