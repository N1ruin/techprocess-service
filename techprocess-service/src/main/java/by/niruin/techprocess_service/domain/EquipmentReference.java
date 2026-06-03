package by.niruin.techprocess_service.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentReference {
    private String name;
    private String index;
    private String standard;
    private Boolean isFromLibrary;
}
