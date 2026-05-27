package by.niruin.techprocess_service.controller;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessResponse;
import by.niruin.techprocess_service.model.technological_process.TechnologicalProcessDto;
import by.niruin.techprocess_service.model.technological_process.UpdateTechprocessRequest;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/techprocess-service")
public class TechnologicalProcessController {
    private final TechnologicalProcessMapper technologicalProcessMapper;
    private final TechnologicalProcessService technologicalProcessService;

    public TechnologicalProcessController(TechnologicalProcessMapper technologicalProcessMapper,
                                          TechnologicalProcessService technologicalProcessService) {
        this.technologicalProcessMapper = technologicalProcessMapper;
        this.technologicalProcessService = technologicalProcessService;
    }

    @PostMapping("/technological-processes")
    public ResponseEntity<CreateTechprocessResponse> save(@Valid @RequestBody CreateTechprocessRequest request) {
        var techprocess = technologicalProcessMapper.toTechnologicalProcess(request);

        var saved = technologicalProcessService.save(techprocess);

        var response = technologicalProcessMapper.toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping

    @GetMapping("/technological-processes/{full-number}")
    public ResponseEntity<TechnologicalProcessDto> getByNumber(@PathVariable("full-number") String fullNumber,
                                                               @RequestParam(required = false) Integer revision) {
        var techprocess = revision == null
                ? technologicalProcessService.getInStatusSetUpByNumber(fullNumber)
                : technologicalProcessService.getByNumberAndRevision(fullNumber, revision);

        var dto = technologicalProcessMapper.toDto(techprocess);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/technological-processes")
    public ResponseEntity<Page<TechnologicalProcessDto>> getPageByStatusSetUp(
            @PageableDefault(sort = "archiveNumber", direction = Sort.Direction.DESC) Pageable pageable) {

        var techprocessPage = technologicalProcessService.getPageByStatus(TechnologicalProcessStatus.SET_UP, pageable);

        var dtosPage = techprocessPage.map(technologicalProcessMapper::toDto);

        return ResponseEntity.ok(dtosPage);
    }

    @PostMapping("/technological-processes/{full-number}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("full-number") String fullNumber) {
        technologicalProcessService.cancel(fullNumber);

        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/techonoligal-processes/{full-number}")
    public ResponseEntity<TechnologicalProcessDto> update(@PathVariable("full-number") String fullNumber,
                                                          @Valid @RequestBody UpdateTechprocessRequest request) {
        var techprocess = technologicalProcessMapper.toTechnologicalProcess(request);

        var updated = technologicalProcessService.update(techprocess);

        var response = technologicalProcessMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }
}
