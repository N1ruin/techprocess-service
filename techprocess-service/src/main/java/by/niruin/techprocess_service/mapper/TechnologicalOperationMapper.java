package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalOperation;
import by.niruin.techprocess_service.model.technological_process.AddOperationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TechnologicalOperationMapper {
    @Mapping(target = "equipment", source = "equipmentReference")
    @Mapping(target = "safetyInstructions", source = "safetyInstructionReferences")
    TechnologicalOperation toOperation(AddOperationRequest request);
}
