package by.niruin.techprocess_service.controller;

import by.niruin.techprocess_service.converter.TechnologicalProcessRequestConverter;
import by.niruin.techprocess_service.converter.TechnologicalProcessConverter;
import by.niruin.techprocess_service.model.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.CreateTechprocessResponse;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/")
public class TechnologicalProcessController {
    private final TechnologicalProcessService technologicalProcessService;
    private final TechnologicalProcessConverter dtoConverter;
    private final TechnologicalProcessRequestConverter processConverter;

    public TechnologicalProcessController(TechnologicalProcessService technologicalProcessService,
                                          TechnologicalProcessConverter dtoConverter,
                                          TechnologicalProcessRequestConverter processConverter) {
        this.technologicalProcessService = technologicalProcessService;
        this.dtoConverter = dtoConverter;
        this.processConverter = processConverter;
    }

    @PostMapping("techprocess/save")
    public ResponseEntity<CreateTechprocessResponse> createProcess(@Valid CreateTechprocessRequest request) {
        var techprocess = processConverter.convert(request);

        var saved = technologicalProcessService.save(techprocess);

        var response = dtoConverter.convert(saved);

        return ResponseEntity.ok(response);
    }
}
