package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalOperation;
import by.niruin.techprocess_service.model.technological_process.AddOperationRequest;
import by.niruin.techprocess_service.model.technological_process.TechnologicalOperationDto;
import by.niruin.techprocess_service.model.technological_process.UpdateOperationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = EquipmentMapper.class)
public interface TechnologicalOperationMapper {
    TechnologicalOperation toOperation(AddOperationRequest request);

    TechnologicalOperation toOperation(UpdateOperationRequest request);

    TechnologicalOperationDto toDto(TechnologicalOperation operation);
}
