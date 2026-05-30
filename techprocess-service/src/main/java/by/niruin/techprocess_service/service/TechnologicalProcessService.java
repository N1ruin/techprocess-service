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
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;
    private final TechnologicalProcessFullNumberBuilder fullNumberBuilder;
    private final EventPublisher eventPublisher;
    private final JwtParser jwtParser;
    private static final Logger log = LogManager.getLogger(TechnologicalProcessService.class);

    public TechnologicalProcessService(TechnologicalProcessRepository repository,
                                       TechnologicalProcessFullNumberBuilder fullNumberBuilder,
                                       EventPublisher eventPublisher, JwtParser jwtParser) {
        this.repository = repository;
        this.fullNumberBuilder = fullNumberBuilder;
        this.eventPublisher = eventPublisher;
        this.jwtParser = jwtParser;
    }

    public TechnologicalProcess save(TechnologicalProcess techprocess) {
        validateUniqueness(techprocess);

        fillMetadata(techprocess);
        fillDeveloper(techprocess);

        checkSelfReview(techprocess);

        var saved = repository.save(techprocess);

        eventPublisher.publishTechprocessCreatedEvent(saved);

        return saved;
    }

    public void cancel(String fullNumber) {
        var existing = repository.findFirstByFullNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s not found"
                        .formatted(fullNumber)));

        if (existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION) {
            throw new TechprocessCancellationException(("Techprocess with number %s cancellation error." +
                    " You must complete or delete the correction revision").formatted(fullNumber));
        }

        if (existing.getStatus() == TechnologicalProcessStatus.CANCELLED
                || existing.getStatus() == TechnologicalProcessStatus.PRODUCTION
                || existing.getStatus() == TechnologicalProcessStatus.IN_DEVELOPMENT
                || existing.getStatus() == TechnologicalProcessStatus.IN_REVIEW) {
            throw new TechprocessCancellationException("It is not possible to cancel the technological process in the status of %s"
                    .formatted(existing.getStatus().name()));
        }

        existing.setStatus(TechnologicalProcessStatus.CANCELLED);

        repository.save(existing);
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

    public TechnologicalProcess update(TechnologicalProcess technologicalProcess) {
        var fullNumber = fullNumberBuilder.buildFullNumber(
                technologicalProcess.getOrganizationType(),
                technologicalProcess.getWorkType(),
                technologicalProcess.getArchiveNumber());

        var existing = repository.findFirstByFullNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесс с id %s не найден".formatted(technologicalProcess.getFullNumber())));

        var status = existing.getStatus();
        if (status != TechnologicalProcessStatus.IN_DEVELOPMENT && status != TechnologicalProcessStatus.IN_CORRECTION) {
            throw new TechprocessUpdatingException("Техпроцесс можно обновить только в статусе \"В разработке\" или \"На корректировке\"");
        }

        if (existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION
                && !existing.getWorkshopCode().equals(technologicalProcess.getWorkshopCode())) {
            throw new TechprocessUpdatingException("Нельзя изменить цех в корректировке");
        }

        updateFields(existing, technologicalProcess);

        repository.save(existing);
        eventPublisher.publishTechprocessUpdatedEvent(existing);

        return existing;
    }

    public TechnologicalProcess addOperation(String fullNumber, TechnologicalOperation operation) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber,
                        TechnologicalProcessStatus.IN_CORRECTION)
                .orElse(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                                fullNumber,
                                TechnologicalProcessStatus.IN_DEVELOPMENT)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Техпроцесса с номером %s и статусом \"В разработке\" или \"На корректировке\" не найдено"
                                        .formatted(fullNumber))));
//todo доделать чтобы у меня была возможность добавлять примечания в техпроцессах типа звездочки поступает в сборе
        checkTechprocessOwner(existing);

        existing.addOperation(operation);
        //проверить что меняется дата обновления если добавить операцию
        return repository.save(existing);
    }

    public TechnologicalProcess addTransition(String operationNumber, String fullNumber, TechnologicalTransition transition) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber,
                        TechnologicalProcessStatus.IN_CORRECTION)
                .orElse(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                                fullNumber,
                                TechnologicalProcessStatus.IN_DEVELOPMENT)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Техпроцесса с номером %s и статусом \"В разработке\" или \"На корректировке\" не найдено"
                                        .formatted(fullNumber))));

        checkTechprocessOwner(existing);

        var existingOperation = existing.getOperations()
                .stream()
                .filter(operation -> operation.getNumber().equals(operationNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Операции с номером %s не существует в техпроцессе."
                        .formatted(operationNumber)));

        existingOperation.addTransition(transition);

        return repository.save(existing);
    }

    public void sendToReview(String fullNumber) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber,
                        TechnologicalProcessStatus.IN_CORRECTION)
                .orElse(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                                fullNumber,
                                TechnologicalProcessStatus.IN_DEVELOPMENT)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Техпроцесса с номером %s и статусом \"В разработке\" или \"На корректировке\" не найдено"
                                        .formatted(fullNumber))));

        checkTechprocessOwner(existing);

        existing.setStatus(TechnologicalProcessStatus.IN_REVIEW);
        existing.setSentToReviewDate(Instant.now());

        eventPublisher.publishProcessSentToReviewEvent(existing);

        repository.save(existing);
    }

    public void approve(String fullNumber) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber,
                        TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() ->
                        new EntityNotFoundException("Техпроцесса с номером %s и статусом \"На проверке\" не найдено"
                                .formatted(fullNumber)));

        checkTechprocessReviewer(existing);

        existing.setStatus(TechnologicalProcessStatus.SET_UP);
        existing.setReviewerApprovedDate(Instant.now());

        eventPublisher.publishProcessSentToReviewEvent(existing);

        repository.save(existing);
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

    private void updateFields(TechnologicalProcess existing, TechnologicalProcess newTechprocess) {
        existing.setPartNumber(newTechprocess.getPartNumber());
        existing.setPartName(newTechprocess.getPartName());
        existing.setWorkshopCode(newTechprocess.getWorkshopCode());
        existing.setOrganizationType(newTechprocess.getOrganizationType());
        existing.setWorkName(newTechprocess.getWorkName());
        existing.setReviewerFirstName(newTechprocess.getReviewerFirstName());
        existing.setReviewerLastName(newTechprocess.getReviewerLastName());
        existing.setReviewerFatherName(newTechprocess.getReviewerFatherName());
        existing.setOperations(newTechprocess.getOperations());

        if (existing.getOrganizationType() != newTechprocess.getOrganizationType()) {
            existing.setOrganizationType(newTechprocess.getOrganizationType());
        }

        existing.setFullNumber(fullNumberBuilder.buildFullNumber(
                newTechprocess.getOrganizationType(),
                newTechprocess.getWorkType(),
                newTechprocess.getArchiveNumber()));
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
}
