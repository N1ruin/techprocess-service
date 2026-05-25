package by.niruin.techprocess_service.controller;

import by.niruin.techprocess_service.converter.TechnologicalProcessRequestConverter;
import by.niruin.techprocess_service.converter.TechnologicalProcessConverter;
import by.niruin.techprocess_service.model.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.CreateTechprocessResponse;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/techprocess-service")
public class TechnologicalProcessController {
    private final TechnologicalProcessService technologicalProcessService;
    private final TechnologicalProcessConverter dtoConverter;
    private final TechnologicalProcessRequestConverter requestConverter;

    public TechnologicalProcessController(TechnologicalProcessService technologicalProcessService,
                                          TechnologicalProcessConverter dtoConverter,
                                          TechnologicalProcessRequestConverter requestConverter) {
        this.technologicalProcessService = technologicalProcessService;
        this.dtoConverter = dtoConverter;
        this.requestConverter = requestConverter;
    }

    @PostMapping("/techprocesses")
    public ResponseEntity<CreateTechprocessResponse> create(@Valid @RequestBody CreateTechprocessRequest request) {
        var techprocess = requestConverter.convert(request);

        var saved = technologicalProcessService.create(techprocess);

        var response = dtoConverter.convert(saved);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/techprocesses/{archiveNumber}")
    public ResponseEntity<CreateTechprocessResponse> getByArchiveNumber(@PathVariable String archiveNumber) {
        var entity = technologicalProcessService.getByArchiveNumber(archiveNumber);
        return ResponseEntity.ok(dtoConverter.convert(entity));
    }

    @PutMapping("/techprocesses/{archiveNumber}")
    public ResponseEntity<CreateTechprocessResponse> update(@PathVariable String archiveNumber,
                                                            @Valid @RequestBody CreateTechprocessRequest request) {
        var entity = requestConverter.convert(request);
        if (archiveNumber != null) {
            entity.setArchiveNumber(archiveNumber);

        }

        var updated = technologicalProcessService.update(entity);

        return ResponseEntity.ok(dtoConverter.convert(updated));
    }

    @PostMapping("/techprocesses/{archiveNumber}/to-setup")
    public ResponseEntity<CreateTechprocessResponse> toSetUp(@PathVariable String archiveNumber) {
        var result = technologicalProcessService.toSetUp(archiveNumber);

        return ResponseEntity.ok(dtoConverter.convert(result));
    }

    @PostMapping("/techprocesses/{archiveNumber}/revision")
    public ResponseEntity<CreateTechprocessResponse> createRevision(@PathVariable String archiveNumber) {
        var revision = technologicalProcessService.createRevision(archiveNumber);

        return ResponseEntity.ok(dtoConverter.convert(revision));
    }

    @PostMapping("/techprocesses/{archiveNumber}/complete-correction")
    public ResponseEntity<CreateTechprocessResponse> completeCorrection(@PathVariable String archiveNumber) {
        var result = technologicalProcessService.completeCorrection(archiveNumber);

        return ResponseEntity.ok(dtoConverter.convert(result));
    }

    @DeleteMapping("/techprocesses/{archiveNumber}/revision")
    public ResponseEntity<Void> deleteRevision(@PathVariable String archiveNumber) {
        technologicalProcessService.deleteRevision(archiveNumber);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/techprocesses/{archiveNumber}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String archiveNumber) {
        technologicalProcessService.cancel(archiveNumber);

        return ResponseEntity.noContent().build();
    }
}
