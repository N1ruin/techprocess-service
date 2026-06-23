package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;
import org.springframework.stereotype.Component;

@Component
public class TechnologicalProcessFullNumberBuilder {
    private static final String WORKS_CODE = "100316761292";
    private static final String NUMBER_DELIMITER = ".";
    private static final String TECHNOLOGICAL_PROCESS_DOCUMENT_SET_CODE = "02";

    public String buildFullNumber(TechnologicalProcessOrganizationType organizationType,
                                  TechnologicalProcessWorkType workType,
                                  String archiveNumber) {
        return WORKS_CODE +
                NUMBER_DELIMITER +
                TECHNOLOGICAL_PROCESS_DOCUMENT_SET_CODE +
                organizationType.getCode() +
                workType.getCode() +
                NUMBER_DELIMITER +
                archiveNumber;
    }
}
