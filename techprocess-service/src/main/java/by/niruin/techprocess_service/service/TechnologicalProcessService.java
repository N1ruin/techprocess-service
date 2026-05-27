package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.exception.EntityAlreadyExistException;
import by.niruin.techprocess_service.exception.EntityNotFoundException;
import by.niruin.techprocess_service.exception.TechprocessCancellationException;
import by.niruin.techprocess_service.exception.TechprocessUpdatingException;
import by.niruin.techprocess_service.kafka.EventPublisher;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.security.JwtParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;
    private final TechnologicalProcessFullNumberBuilder fullNumberBuilder;
    private final EventPublisher eventPublisher;
    private final JwtParser jwtParser;

    public TechnologicalProcessService(TechnologicalProcessRepository repository,
                                       TechnologicalProcessFullNumberBuilder fullNumberBuilder,
                                       EventPublisher eventPublisher, JwtParser jwtParser) {
        this.repository = repository;
        this.fullNumberBuilder = fullNumberBuilder;
        this.eventPublisher = eventPublisher;
        this.jwtParser = jwtParser;
    }

    @Transactional
    public TechnologicalProcess save(TechnologicalProcess techprocess) {
        validateUniqueness(techprocess);

        fillMetadata(techprocess);
        fillDeveloper(techprocess);

        var saved = repository.save(techprocess);

        eventPublisher.publishTechprocessCreatedEvent(saved);

        return saved;
    }

    @Transactional
    public void cancel(String fullNumber) {
        var existing = repository.findFirstByArchiveNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s not found"
                        .formatted(fullNumber)));

        if (existing.getStatus() == TechnologicalProcessStatus.IN_CORRECTION) {
            throw new TechprocessCancellationException(("Techprocess with number %s cancellation error." +
                    " You must complete or delete the correction revision").formatted(fullNumber));
        }

        if (existing.getStatus() == TechnologicalProcessStatus.CANCELLED
                || existing.getStatus() == TechnologicalProcessStatus.PRODUCTION) {
            throw new TechprocessCancellationException("It is not possible to cancel the technological process in the status of %s"
                    .formatted(existing.getStatus().name()));

        }
        existing.setStatus(TechnologicalProcessStatus.CANCELLED);

        repository.save(existing);
        eventPublisher.publishTechprocessCancelledEvent(existing);
    }

    @Transactional(readOnly = true)
    public TechnologicalProcess getInStatusSetUpByNumber(String fullNumber) {
        return repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(fullNumber, TechnologicalProcessStatus.SET_UP)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Techprocess with number %s not found".formatted(fullNumber)));
    }

    @Transactional(readOnly = true)
    public TechnologicalProcess getByNumberAndRevision(String fullNumber, Integer revision) {
        return repository.findByFullNumberAndRevision(fullNumber, revision)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s and revision %d not found"
                        .formatted(fullNumber, revision)));
    }

    @Transactional(readOnly = true)
    public Page<TechnologicalProcess> getPage(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<TechnologicalProcess> getPageByStatus(TechnologicalProcessStatus technologicalProcessStatus,
                                                      Pageable pageable) {
        return repository.findAllByStatus(technologicalProcessStatus, pageable);
    }

    @Transactional
    public TechnologicalProcess update(TechnologicalProcess technologicalProcess) {
        var fullNumber = fullNumberBuilder.buildFullNumber(
                technologicalProcess.getOrganizationType(),
                technologicalProcess.getWorkType(),
                technologicalProcess.getArchiveNumber());

        var existing = repository.findFirstByFullNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Techprocess with number %s not found".formatted(technologicalProcess.getFullNumber())));

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

        return existing;
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

    private void checkIsNumberExistInWorkshop(String organizationType, String workType, String archiveNumber, String workshopCode) {
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

    }
}
