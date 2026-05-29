package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TechnologicalTransition;
import by.niruin.techprocess_service.model.technological_process.AddTransitionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TechnologicalTransitionMapper {
    TechnologicalTransition toTransition(AddTransitionRequest request);
}
