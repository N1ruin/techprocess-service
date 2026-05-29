package by.niruin.techprocess_service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SafetyInstructionReference {
    private String number;
    @JsonProperty("fromLibrary")
    private boolean isFromLibrary;

    public SafetyInstructionReference(String number, boolean isFromLibrary) {
        this.number = number;
        this.isFromLibrary = isFromLibrary;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isFromLibrary() {
        return isFromLibrary;
    }

    public void setFromLibrary(boolean fromLibrary) {
        isFromLibrary = fromLibrary;
    }
}
