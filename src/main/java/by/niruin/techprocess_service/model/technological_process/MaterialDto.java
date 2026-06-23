package by.niruin.techprocess_service.model.technological_process;

import lombok.Builder;

@Builder
public record MaterialDto(String position,
                          String name,
                          Boolean isFromLibrary,
                          String supplierCode,
                          String standard,
                          String unit,
                          Integer rationingUnit,
                          Double consumptionRate,
                          String note) {
}
