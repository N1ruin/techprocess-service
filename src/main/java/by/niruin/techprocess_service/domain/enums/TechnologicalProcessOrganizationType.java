package by.niruin.techprocess_service.domain.enums;

import lombok.Getter;

@Getter
public enum TechnologicalProcessOrganizationType {
    SINGLE("1"),
    TYPICAL("2"),
    GROUP("3");

    private final String code;

    TechnologicalProcessOrganizationType(String code) {
        this.code = code;
    }

}
