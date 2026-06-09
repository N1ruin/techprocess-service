package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.Part;
import by.niruin.techprocess_service.model.technological_process.PartDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PartMapper {
    PartDto toDto(Part part);
}
