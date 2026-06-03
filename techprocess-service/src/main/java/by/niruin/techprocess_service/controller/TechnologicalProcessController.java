package by.niruin.techprocess_service.controller;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalOperationMapper;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.technological_process.*;
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
@RequestMapping("/api/v1/techprocess-service")
public class TechnologicalProcessController {
    private final TechnologicalProcessMapper technologicalProcessMapper;
    private final TechnologicalProcessService technologicalProcessService;
    private final TechnologicalOperationMapper technologicalOperationMapper;

    public TechnologicalProcessController(TechnologicalProcessMapper technologicalProcessMapper,
                                          TechnologicalProcessService technologicalProcessService,
                                          TechnologicalOperationMapper technologicalOperationMapper) {
        this.technologicalProcessMapper = technologicalProcessMapper;
        this.technologicalProcessService = technologicalProcessService;
        this.technologicalOperationMapper = technologicalOperationMapper;
    }

    //ПРОТЕСТИРОВАН
    @PostMapping("/technological-processes")
    public ResponseEntity<CreateTechprocessResponse> create(@Valid @RequestBody CreateTechprocessRequest request) {
        var techprocess = technologicalProcessMapper.toTechnologicalProcess(request);

        var saved = technologicalProcessService.save(techprocess);

        var response = technologicalProcessMapper.toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    //ПРОТЕСТИРОВАН
    @GetMapping("/technological-processes/{full-number}")
    public ResponseEntity<TechnologicalProcessDto> getByFullNumberAndRevision(@PathVariable("full-number") String fullNumber,
                                                                              @RequestParam(required = false) Integer revision) {
        var techprocess = revision == null
                ? technologicalProcessService.getInStatusSetUpByNumber(fullNumber)
                : technologicalProcessService.getByNumberAndRevision(fullNumber, revision);

        var dto = technologicalProcessMapper.toDto(techprocess);

        return ResponseEntity.ok(dto);
    }

    //ПРОТЕСТИРОВАН
    @GetMapping("/technological-processes")
    public ResponseEntity<Page<TechnologicalProcessDto>> getPageInStatusSetUp(
            @PageableDefault(sort = "archiveNumber", direction = Sort.Direction.DESC) Pageable pageable) {

        var techprocessPage = technologicalProcessService.getPageByStatus(TechnologicalProcessStatus.SET_UP, pageable);

        var dtosPage = techprocessPage.map(technologicalProcessMapper::toDto);

        return ResponseEntity.ok(dtosPage);
    }

    //ПРОТЕСТИРОВАН
    @PostMapping("/technological-processes/{full-number}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("full-number") String fullNumber) {
        technologicalProcessService.cancel(fullNumber);

        return ResponseEntity.ok()
                .build();
    }

    @PutMapping("/technological-processes/{full-number}")
    public ResponseEntity<TechnologicalProcessDto> update(@PathVariable("full-number") String fullNumber,
                                                          @Valid @RequestBody UpdateTechprocessRequest request) {
        var techprocess = technologicalProcessMapper.toTechnologicalProcess(request);

        var updated = technologicalProcessService.update(fullNumber, techprocess);

        var response = technologicalProcessMapper.toDto(updated);

        return ResponseEntity.ok(response);
    }

    //протестирован
    @PostMapping("/technological-processes/{full-number}/operations")
    public ResponseEntity<TechnologicalProcessDto> addOperation(@PathVariable("full-number") String fullNumber,
                                                                @Valid @RequestBody AddOperationRequest request) {
        var operation = technologicalOperationMapper.toOperation(request);
        operation.setParts(request.partReferences());

        var result = technologicalProcessService.addOperation(fullNumber, operation);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(technologicalProcessMapper.toDto(result));
    }
//протестирован
    @DeleteMapping("technological-processes/{full-number}/operations/{operation-number}")
    public ResponseEntity<Void> deleteOperation(@PathVariable("full-number") String fullNumber,
                                                @PathVariable("operation-number") String operationNumber) {
        technologicalProcessService.deleteOperation(fullNumber, operationNumber);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @PutMapping("technological-processes/{full-number}/operations/{operation-number}")
    public ResponseEntity<TechnologicalProcessDto> updateOperation(@PathVariable("full-number") String fullNumber,
                                                                   @PathVariable("operation-number") String operationNumber,
                                                                   @Valid @RequestBody UpdateOperationRequest request) {
        var operation = technologicalOperationMapper.toOperation(request);

        var techprocess = technologicalProcessService.updateOperation(fullNumber, operationNumber, operation);

        var dto = technologicalProcessMapper.toDto(techprocess);

        return ResponseEntity.ok(dto);
    }

    // протестировано
    @PostMapping("/technological-processes/{full-number}/send-to-review")
    public ResponseEntity<Void> sendToReview(@PathVariable("full-number") String fullNumber) {
        technologicalProcessService.sendToReview(fullNumber);

        return ResponseEntity.ok().build();
    }

    //протестировано
    @PostMapping("/technological-processes/{full-number}/approve")
    public ResponseEntity<Void> approve(@PathVariable("full-number") String fullNumber) {
        technologicalProcessService.approve(fullNumber);

        return ResponseEntity.ok().build();
    }

//    @PostMapping("/technological-processes/{full-number}/return-for-revision")
//    public ResponseEntity<TechnologicalProcessDto> returnForRevision(@PathVariable("full-number") String fullNumber,
//                                                                     @Valid @RequestBody AddCommentRequest request) {
//        var result = technologicalProcessService.returnForRevision(fullNumber, request.content());
//
//        return ResponseEntity.ok(technologicalProcessMapper.toDto(result));
//    }

//    @PostMapping("/technological-processes/{full-number}/comments/{comment-id}/resolve")
//    public ResponseEntity<TechnologicalProcessDto> resolveComment(@PathVariable("full-number") String fullNumber,
//                                                                  @PathVariable("comment-id") String commentId) {
//        var result = technologicalProcessService.resolveComment(fullNumber, commentId);
//        return ResponseEntity.ok(technologicalProcessMapper.toDto(result));
//    }
//
//    @PostMapping("/technological-processes/{full-number}/create-revision")
//    public ResponseEntity<Void> createRevision(@PathVariable("full-number") String fullNumber) {
//        technologicalProcessService.createRevision(fullNumber);
//
//        return ResponseEntity.status(HttpStatus.CREATED).build();
//    }

}
