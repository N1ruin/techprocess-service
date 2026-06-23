package by.niruin.techprocess_service.model.technological_process;

import lombok.Builder;

@Builder
public record EquipmentDto(String name,
                           String index,
                           String standard,
                           Boolean isFromLibrary) {
}
