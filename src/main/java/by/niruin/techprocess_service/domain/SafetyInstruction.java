package by.niruin.techprocess_service.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SafetyInstruction {
    private String number;
    private Boolean isFromLibrary;
}
