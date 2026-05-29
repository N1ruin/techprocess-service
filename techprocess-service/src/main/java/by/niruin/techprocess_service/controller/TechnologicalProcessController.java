package by.niruin.techprocess_service.controller;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalOperationMapper;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.mapper.TechnologicalTransitionMapper;
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
    private final TechnologicalTransitionMapper technologicalTransitionMapper;

    public TechnologicalProcessController(TechnologicalProcessMapper technologicalProcessMapper,
                                          TechnologicalProcessService technologicalProcessService,
                                          TechnologicalOperationMapper technologicalOperationMapper,
                                          TechnologicalTransitionMapper technologicalTransitionMapper) {
        this.technologicalProcessMapper = technologicalProcessMapper;
        this.technologicalProcessService = technologicalProcessService;
        this.technologicalOperationMapper = technologicalOperationMapper;
        this.technologicalTransitionMapper = technologicalTransitionMapper;
    }

    //ПРОТЕСТИРОВАН
    @PostMapping("/technological-processes")
    public ResponseEntity<CreateTechprocessResponse> save(@Valid @RequestBody CreateTechprocessRequest request) {
        var techprocess = technologicalProcessMapper.toTechnologicalProcess(request);

        var saved = technologicalProcessService.save(techprocess);

        var response = technologicalProcessMapper.toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    //ПРОТЕСТИРОВАН
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

    //ПРОТЕСТИРОВАН 1 КЕЙС, ЕЩЕ 3
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

    @PostMapping("/technological-processes/{full-number}/operations")
    public ResponseEntity<TechnologicalProcessDto> addOperation(@PathVariable("full-number") String fullNumber,
                                                                @Valid @RequestBody AddOperationRequest request) {
        var operation = technologicalOperationMapper.toOperation(request);

        var result = technologicalProcessService.addOperation(fullNumber, operation);

        return ResponseEntity.ok(technologicalProcessMapper.toDto(result));
    }

    @PostMapping("/technological-processes/{full-number}/operations/{number}")
    public ResponseEntity<TechnologicalProcessDto> addTransition(@PathVariable("full-number") String fullNumber,
                                                                 @Valid @RequestBody AddTransitionRequest request) {
        var transition = technologicalTransitionMapper.toTransition(request);

        var result = technologicalProcessService.addTransition(request.operationNumber(), fullNumber, transition);

        return ResponseEntity.ok(technologicalProcessMapper.toDto(result));
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
