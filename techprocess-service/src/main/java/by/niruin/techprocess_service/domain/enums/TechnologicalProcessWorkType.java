package by.niruin.techprocess_service.domain.enums;

public enum TechnologicalProcessWorkType {
    ASSEMBLY("88"),
    GENERAL_PURPOSE("01"),
    TESTS("06");

    private final String code;

    TechnologicalProcessWorkType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
