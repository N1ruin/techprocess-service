package by.niruin.techprocess_service.domain.enums;

public enum OperationType {
    ASSEMBLY("88"),
    CONTROL("03");

    private final String code;

    OperationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
