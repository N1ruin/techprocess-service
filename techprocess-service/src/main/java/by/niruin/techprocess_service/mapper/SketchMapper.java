package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.Sketch;
import by.niruin.techprocess_service.model.technological_process.SketchDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SketchMapper {
    SketchDto toDto(Sketch sketch);
}
