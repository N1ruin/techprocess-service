package by.niruin.techprocess_service.model.technological_process;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SafetyInstructionDto(String number,
                                   @JsonProperty("fromLibrary")
                                   Boolean isFromLibrary) {
}
