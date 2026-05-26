package by.niruin.techprocess_service.domain;

public class SafetyInstructionReference {
    private String number;
    private boolean isFromLibrary;

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
