package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.SafetyInstruction;
import by.niruin.techprocess_service.model.technological_process.SafetyInstructionDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SafetyInstructionMapper {
    SafetyInstructionDto toDto(SafetyInstruction safetyInstruction);

    List<SafetyInstructionDto> toDtoList(List<SafetyInstruction> safetyInstructions);
}
