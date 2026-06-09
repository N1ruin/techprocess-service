package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalTransition;
import by.niruin.techprocess_service.model.technological_process.TechnologicalTransitionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = EquipmentMapper.class)
public interface TechnologicalTransitionMapper {
    TechnologicalTransitionDto toDto(TechnologicalTransition technologicalTransition);
}
