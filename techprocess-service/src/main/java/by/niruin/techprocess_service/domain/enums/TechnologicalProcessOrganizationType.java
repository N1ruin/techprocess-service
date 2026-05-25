package by.niruin.techprocess_service.domain.enums;

public enum TechnologicalProcessOrganizationType {
    SINGLE("1"),
    TYPICAL("2"),
    GROUP("3");

    private final String code;

    TechnologicalProcessOrganizationType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
