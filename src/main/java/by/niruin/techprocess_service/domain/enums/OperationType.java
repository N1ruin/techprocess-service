package by.niruin.techprocess_service.domain.enums;

import lombok.Getter;

@Getter
public enum OperationType {
    ASSEMBLY("88"),
    CONTROL("03");

    private final String code;

    OperationType(String code) {
        this.code = code;
    }

}
