package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalOperation;
import by.niruin.techprocess_service.model.technological_process.CreateOperationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TechnologicalOperationMapper {
    TechnologicalOperation toOperation(CreateOperationRequest request);
}
