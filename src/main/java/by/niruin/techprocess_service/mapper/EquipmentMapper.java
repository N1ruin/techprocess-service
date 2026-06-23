package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.Equipment;
import by.niruin.techprocess_service.model.technological_process.EquipmentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {
    EquipmentDto toDto(Equipment equipment);
}
