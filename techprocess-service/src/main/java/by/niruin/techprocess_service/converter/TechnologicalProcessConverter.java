package by.niruin.techprocess_service.converter;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.model.CreateTechprocessResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TechnologicalProcessConverter implements Converter<TechnologicalProcess, CreateTechprocessResponse> {
    @Override
    public CreateTechprocessResponse convert(TechnologicalProcess process) {
        return new CreateTechprocessResponse(
                process.getId(),
                process.getName(),
                process.getPartNumber(),
                process.getArchiveNumber(),
                process.getWorkshopCode(),
                process.getType().name(),
                process.getStatus().name(),
                process.getRevision(),
                process.getWorkType(),
                process.getCreatedDate(),
                process.getUpdatedDate());
    }
}
