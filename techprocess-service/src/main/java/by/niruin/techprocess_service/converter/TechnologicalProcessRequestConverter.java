package by.niruin.techprocess_service.converter;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.model.CreateTechprocessRequest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TechnologicalProcessRequestConverter implements Converter<CreateTechprocessRequest, TechnologicalProcess> {
    @Override
    public TechnologicalProcess convert(CreateTechprocessRequest request) {
        var process = new TechnologicalProcess();
        process.setName(request.name());
        process.setPartNumber(request.partNumber());
        process.setArchiveNumber(request.archiveNumber());
        process.setWorkshopCode(request.workshopCode());
        process.setType(TechnologicalProcessOrganizationType.valueOf(request.type()));
        return process;
    }
}
