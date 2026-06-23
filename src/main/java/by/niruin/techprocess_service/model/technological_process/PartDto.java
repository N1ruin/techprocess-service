package by.niruin.techprocess_service.model.technological_process;

import lombok.Builder;

@Builder
public record PartDto(String position,
                      String name,
                      String number,
                      String supplierCode,
                      String materialUnit,
                      Integer quantity,
                      String note) {
}
