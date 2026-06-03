package by.niruin.techprocess_service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SafetyInstructionReference {
    private String number;
    @JsonProperty("fromLibrary")
    private Boolean isFromLibrary;
}
