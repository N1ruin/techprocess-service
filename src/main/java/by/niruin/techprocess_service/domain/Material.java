package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.MaterialUnit;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {
    private String position;
    private String name;
    private Boolean isFromLibrary;
    private String supplierCode;
    private String standard;
    private MaterialUnit unit;
    private Integer rationingUnit;
    private Double consumptionRate;
    private String note;
}
