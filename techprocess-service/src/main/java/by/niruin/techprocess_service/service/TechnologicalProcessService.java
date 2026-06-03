package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.TechnologicalOperation;
import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.TechnologicalTransition;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.exception.*;
import by.niruin.techprocess_service.kafka.EventPublisher;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.security.JwtParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;
    private final TechnologicalProcessFullNumberBuilder fullNumberBuilder;
    private final EventPublisher eventPublisher;
    private final JwtParser jwtParser;
    private final MongoTemplate mongoTemplate;
    private static final Logger log = LogManager.getLogger(TechnologicalProcessService.class);

    public TechnologicalProcessService(TechnologicalProcessRepository repository,
                                       TechnologicalProcessFullNumberBuilder fullNumberBuilder,
                                       EventPublisher eventPublisher,
                                       JwtParser jwtParser,
                                       MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.fullNumberBuilder = fullNumberBuilder;
        this.eventPublisher = eventPublisher;
        this.jwtParser = jwtParser;
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public TechnologicalProcess save(TechnologicalProcess techprocess) {
        validateUniqueness(techprocess);

        fillMetadata(techprocess);
        fillDeveloper(techprocess);

        checkSelfReview(techprocess);

        var saved = repository.save(techprocess);

        eventPublisher.publishTechprocessCreatedEvent(saved);

        return saved;
    }

    @Transactional
    public void cancel(String fullNumber) {
        var existing = repository.findFirstByFullNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s not found"
                        .formatted(fullNumber)));

        if (existing.getStatus() == TechnologicalProcessStatus.CANCELLED
            || existing.getStatus() == TechnologicalProcessStatus.PRODUCTION
            || existing.getStatus() == TechnologicalProcessStatus.IN_DEVELOPMENT
            || existing.getStatus() == TechnologicalProcessStatus.IN_REVIEW
            || existing.getStatus() == TechnologicalProcessStatus.RETURNING_TO_CORRECTION_AFTER_REVIEW
            || existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION) {
            throw new TechprocessCancellationException("It is not possible to cancel the technological process in the status of %s"
                    .formatted(existing.getStatus().name()));
        }

        checkTechprocessOwner(existing);

        var query = Query.query(Criteria.where("_id").is(existing.getId()));
        var update = new Update()
                .set("status", TechnologicalProcessStatus.CANCELLED);
        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);

        eventPublisher.publishTechprocessCancelledEvent(existing);
    }

    public TechnologicalProcess getInStatusSetUpByNumber(String fullNumber) {
        return repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(fullNumber, TechnologicalProcessStatus.SET_UP)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Techprocess with number %s not found".formatted(fullNumber)));
    }

    public TechnologicalProcess getByNumberAndRevision(String fullNumber, Integer revision) {
        return repository.findByFullNumberAndRevision(fullNumber, revision)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s and revision %d not found"
                        .formatted(fullNumber, revision)));
    }

    public Page<TechnologicalProcess> getPage(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<TechnologicalProcess> getPageByStatus(TechnologicalProcessStatus technologicalProcessStatus,
                                                      Pageable pageable) {
        return repository.findAllByStatus(technologicalProcessStatus, pageable);
    }

    @Transactional
    public TechnologicalProcess update(String fullNumber, TechnologicalProcess newTechprocess) {
        var existing = getEditableProcess(fullNumber);
        checkTechprocessOwner(existing);

        if (existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION
            && !existing.getWorkshopCode().equals(newTechprocess.getWorkshopCode())) {
            throw new TechprocessUpdatingException("Нельзя изменить цех при корректировке");
        }

        validateOperationNumbersUniqueness(newTechprocess.getOperations());

        var query = Query.query(Criteria.where("_id").is(existing.getId()));
        var update = new Update()
                .set("partNumber", newTechprocess.getPartNumber())
                .set("partName", newTechprocess.getPartName())
                .set("workshopCode", newTechprocess.getWorkshopCode())
                .set("organizationType", newTechprocess.getOrganizationType())
                .set("workName", newTechprocess.getWorkName())
                .set("reviewerFirstName", newTechprocess.getReviewerFirstName())
                .set("reviewerLastName", newTechprocess.getReviewerLastName())
                .set("reviewerFatherName", newTechprocess.getReviewerFatherName());

        if (!existing.getOrganizationType().equals(newTechprocess.getOrganizationType())) {
            var newFullNumber = fullNumberBuilder.buildFullNumber(
                    newTechprocess.getOrganizationType(),
                    newTechprocess.getWorkType(),
                    newTechprocess.getArchiveNumber());

            update.set("fullNumber", newFullNumber);
        }

        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);
        eventPublisher.publishTechprocessUpdatedEvent(existing);

        return repository.findById(existing.getId()).get();
    }

    @Transactional
    public TechnologicalProcess addOperation(String fullNumber, TechnologicalOperation operation) {
        var existingTechprocess = getEditableProcess(fullNumber);
        checkTechprocessOwner(existingTechprocess);

        var isOperationNumberExist = existingTechprocess.getOperations()
                .stream()
                .anyMatch(o -> o.getNumber().equals(operation.getNumber()));

        if (isOperationNumberExist) {
            throw new EntityAlreadyExistException("Operation with number %s exist in techprocess %s"
                    .formatted(operation.getNumber(), existingTechprocess.getFullNumber()));
        }

        var query = Query.query(Criteria.where("_id").is(existingTechprocess.getId()));
        var update = new Update()
                .push("operations", operation)
                .set("updatedDate", Instant.now());

        var options = FindAndModifyOptions.options().returnNew(true);

        return mongoTemplate.findAndModify(query, update, options, TechnologicalProcess.class);
    }

    @Transactional
    public void deleteOperation(String fullNumber, String operationNumber) {
        var existingProcess = getEditableProcess(fullNumber);

        var hasOperation = existingProcess.getOperations().stream()
                .anyMatch(op -> op.getNumber().equals(operationNumber));
        if (!hasOperation) {
            throw new EntityNotFoundException("Operation with number %s not found"
                    .formatted(operationNumber));
        }

        checkTechprocessOwner(existingProcess);

        var query = Query.query(
                Criteria.where("_id").is(existingProcess.getId()));
        var update = new Update().pull("operations",
                Query.query(Criteria.where("number").is(operationNumber)));
        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);
    }

    @Transactional
    public TechnologicalProcess updateOperation(String fullNumber, String operationNumber,
                                                TechnologicalOperation newOperation) {
        var existingProcess = getEditableProcess(fullNumber);
        checkTechprocessOwner(existingProcess);

        var operation = existingProcess.getOperations()
                .stream()
                .filter(op -> op.getNumber().equals(operationNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Операция с номером %s не найдена в техпроцессе %s"
                        .formatted(operationNumber, fullNumber)));

        validateTransitionNumbersUniqueness(newOperation);

        var query = Query.query(
                Criteria.where("_id").is(existingProcess.getId())
                        .and("operations.number").is(operationNumber));
        var update = new Update().set("operations.$", newOperation);

        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);

        return repository.findById(existingProcess.getId()).get();
    }

    @Transactional
    public void sendToReview(String fullNumber) {
        var existing = getEditableProcess(fullNumber);
        checkTechprocessOwner(existing);

        var query = Query.query(Criteria.where("_id").is(existing.getId()));
        var update = new Update()
                .set("status", TechnologicalProcessStatus.IN_REVIEW)
                .set("sentToReviewDate", Instant.now())
                .set("updatedDate", Instant.now());
        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);

        eventPublisher.publishProcessSentToReviewEvent(existing);
    }

    @Transactional
    public void approve(String fullNumber) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber,
                        TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() ->
                        new EntityNotFoundException("Техпроцесса с номером %s и статусом \"На проверке\" не найдено"
                                .formatted(fullNumber)));

        checkTechprocessReviewer(existing);

        var query = Query.query(Criteria.where("_id").is(existing.getId()));
        var update = new Update()
                .set("status", TechnologicalProcessStatus.SET_UP)
                .set("reviewerApprovedDate", Instant.now());

        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);

        eventPublisher.publishProcessSentToReviewEvent(existing);
    }

    private TechnologicalProcess getEditableProcess(String fullNumber) {
        return repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber, TechnologicalProcessStatus.IN_CORRECTION)
                .orElseGet(() -> repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                                fullNumber, TechnologicalProcessStatus.IN_DEVELOPMENT)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Техпроцесс с номером %s в статусе \"В разработке\" или \"На корректировке\" не найден"
                                        .formatted(fullNumber))));
    }

    private void checkIsNumberExist(String organizationType, String workType, String archiveNumber) {
        var existing = repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumber(
                organizationType,
                workType,
                archiveNumber);

        if (existing) {
            throw new EntityAlreadyExistException(("Technological process with organization type %s," +
                                                   " work type %s, archive number %s existing")
                    .formatted(organizationType, workType, archiveNumber));
        }
    }

    private void checkIsNumberExistInWorkshop(String organizationType, String workType, String
            archiveNumber, String workshopCode) {
        var existing = repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(
                organizationType,
                workType,
                archiveNumber,
                workshopCode);

        if (existing) {
            throw new EntityAlreadyExistException(("Technological process with organization type %s," +
                                                   " work type %s, archive number %s, workshop code %s existing")
                    .formatted(organizationType, workType, archiveNumber, workshopCode));
        }
    }

    private void validateUniqueness(TechnologicalProcess techprocess) {
        checkIsNumberExist(
                techprocess.getOrganizationType().name(),
                techprocess.getWorkType().name(),
                techprocess.getArchiveNumber());

        checkIsNumberExistInWorkshop(
                techprocess.getOrganizationType().name(),
                techprocess.getWorkType().name(),
                techprocess.getArchiveNumber(),
                techprocess.getWorkshopCode());
    }

    private void checkSelfReview(TechnologicalProcess techprocess) {
        if (techprocess.getDeveloperFirstName().equals(techprocess.getReviewerFirstName()) &&
            techprocess.getDeveloperLastName().equals(techprocess.getReviewerLastName()) &&
            techprocess.getDeveloperFatherName().equals(techprocess.getReviewerFatherName())) {
            throw new TechprocessSavingException("Нельзя назначить на проверяющего самого себя");
        }
    }

    private void fillMetadata(TechnologicalProcess techprocess) {
        techprocess.setStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        techprocess.setRevision(0);
        techprocess.setFullNumber(fullNumberBuilder.buildFullNumber(
                techprocess.getOrganizationType(),
                techprocess.getWorkType(),
                techprocess.getArchiveNumber()));
    }

    private void fillDeveloper(TechnologicalProcess techprocess) {
        techprocess.setDeveloperFirstName(jwtParser.getFirstName());
        techprocess.setDeveloperLastName(jwtParser.getLastName());
        techprocess.setDeveloperFatherName(jwtParser.getFatherName());
    }

    private void checkTechprocessOwner(TechnologicalProcess technologicalProcess) {
        var devFirstName = jwtParser.getFirstName();
        var devLastName = jwtParser.getLastName();
        var devFatherName = jwtParser.getFatherName();

        if (!technologicalProcess.getDeveloperFirstName().equals(devFirstName)
            && !technologicalProcess.getDeveloperLastName().equals(devLastName)
            && !technologicalProcess.getDeveloperFatherName().equals(devFatherName)) {
            throw new AuthorizationException("Нельзя выполнить операцию не являясь владельцем техпроцесса");
        }
    }

    private void checkTechprocessReviewer(TechnologicalProcess technologicalProcess) {
        var reviewerFirstName = jwtParser.getFirstName();
        var reviewerLastName = jwtParser.getLastName();
        var reviewerFatherName = jwtParser.getFatherName();

        if (!technologicalProcess.getReviewerFirstName().equals(reviewerFirstName)
            && !technologicalProcess.getReviewerLastName().equals(reviewerLastName)
            && !technologicalProcess.getReviewerFatherName().equals(reviewerFatherName)) {
            throw new AuthorizationException("Нет прав для согласования техпроцесса");
        }
    }

    private void validateOperationNumbersUniqueness(List<TechnologicalOperation> operations) {
        Set<String> numbers = new HashSet<>();
        for (var op : operations) {
            if (!numbers.add(op.getNumber())) {
                throw new EntityAlreadyExistException(
                        "Операция с номером %s уже существует в техпроцессе".formatted(op.getNumber()));
            }
        }
    }

    private void validateTransitionNumbersUniqueness(TechnologicalOperation operation) {
        if (operation.getTransitions() == null || operation.getTransitions().isEmpty()) {
            return;
        }

        var duplicates = operation.getTransitions().stream()
                .collect(Collectors.groupingBy(TechnologicalTransition::getNumber, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (!duplicates.isEmpty()) {
            throw new EntityAlreadyExistException(
                    "Переходы с номерами %s повторяются в операции %s"
                            .formatted(duplicates, operation.getNumber()));
        }
    }
}
