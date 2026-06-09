package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.Material;
import by.niruin.techprocess_service.model.technological_process.MaterialDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaterialMapper {
    MaterialDto toDto(Material material);

    List<MaterialDto> toDtoList(List<Material> materials);
}
