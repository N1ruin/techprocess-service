package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.MaterialUnit;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Part {
    private String position;
    private String name;
    private String number;
    private String supplierCode;
    private MaterialUnit materialUnit;
    private Integer quantity;
    private String note;
}
