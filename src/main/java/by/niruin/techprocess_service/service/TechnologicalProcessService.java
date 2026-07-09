package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.client.FileServiceClient;
import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.exception.*;
import by.niruin.techprocess_service.kafka.EventPublisher;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.security.JwtParser;
import by.niruin.techprocess_service.util.TechnologicalProcessFullNumberBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TechnologicalProcessService {
    private final TechnologicalProcessRepository repository;
    private final TechnologicalProcessFullNumberBuilder fullNumberBuilder;
    private final EventPublisher eventPublisher;
    private final JwtParser jwtParser;
    private final MongoTemplate mongoTemplate;
    private final FileServiceClient fileServiceClient;
    private final TransactionTemplate transactionTemplate;

    public TechnologicalProcessService(TechnologicalProcessRepository repository,
                                       TechnologicalProcessFullNumberBuilder fullNumberBuilder,
                                       EventPublisher eventPublisher, JwtParser jwtParser, MongoTemplate mongoTemplate,
                                       FileServiceClient fileServiceClient, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.fullNumberBuilder = fullNumberBuilder;
        this.eventPublisher = eventPublisher;
        this.jwtParser = jwtParser;
        this.mongoTemplate = mongoTemplate;
        this.fileServiceClient = fileServiceClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#result.fullNumber")
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
    @CacheEvict(value = "techprocess", key = "#fullNumber")
    public void cancel(String fullNumber) {
        var existing = repository.findFirstByFullNumberOrderByRevisionDesc(fullNumber)
                .orElseThrow(() -> new EntityNotFoundException("Techprocess with number %s not found"
                        .formatted(fullNumber)));

        if (existing.getStatus() != TechnologicalProcessStatus.SET_UP) {
            throw new TechprocessCancellationException(
                    "It is not possible to cancel the technological process in the status of %s"
                            .formatted(existing.getStatus().name()));
        }

        checkTechprocessOwner(existing);
        repository.setStatus(existing.getId(), TechnologicalProcessStatus.CANCELLED);
        eventPublisher.publishTechprocessCancelledEvent(existing);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "techprocess", key = "#fullNumber")
    public TechnologicalProcess getInStatusSetUpByNumber(String fullNumber) {
        return repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(fullNumber, TechnologicalProcessStatus.SET_UP)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Techprocess with number %s not found".formatted(fullNumber)));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "techprocess", key = "#fullNumber + '-' + #revision")
    public TechnologicalProcess getByNumberAndRevision(String fullNumber, Integer revision) {
        return repository.findByFullNumberAndRevision(fullNumber, revision)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Techprocess with number %s and revision %d not found".formatted(fullNumber, revision)));
    }

    @Transactional(readOnly = true)
    public Page<TechnologicalProcess> getPageByStatus(TechnologicalProcessStatus status, Pageable pageable) {
        return repository.findAllByStatus(status, pageable);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "techprocess", key = "#result.fullNumber"),
            evict = @CacheEvict(value = "techprocess", key = "#fullNumber")
    )
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
    @CachePut(value = "techprocess", key = "#result.fullNumber")
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
        var update = new Update().push("operations", operation);

        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), TechnologicalProcess.class);
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#fullNumber")
    public void deleteOperation(String fullNumber, String operationNumber) {
        var existingProcess = getEditableProcess(fullNumber);

        var hasOperation = existingProcess.getOperations().stream()
                .anyMatch(op -> op.getNumber().equals(operationNumber));
        if (!hasOperation) {
            throw new EntityNotFoundException("Operation with number %s not found".formatted(operationNumber));
        }

        checkTechprocessOwner(existingProcess);
        repository.deleteOperation(existingProcess.getId(), operationNumber);
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#result.fullNumber")
    public TechnologicalProcess updateOperation(String fullNumber, String operationNumber,
                                                TechnologicalOperation newOperation,
                                                List<MultipartFile> newSketchFiles) {
        var newSketchFileNames = uploadSketchImages(newSketchFiles);
        setUploadedFileNamesToSketches(newSketchFileNames, newOperation.getSketches());

        return transactionTemplate.execute(status -> {
            var existingProcess = getEditableProcess(fullNumber);
            checkTechprocessOwner(existingProcess);

            var oldOperation = getOperationByNumber(operationNumber, existingProcess);
            validateTransitionNumbersUniqueness(newOperation);
            updateOperationInDatabase(operationNumber, newOperation, existingProcess);

            newSketchFileNames.forEach(eventPublisher::publishFileMovedToPermanentStorageEvent);

            var deletionFileNames = getDeletionFileNames(newOperation, oldOperation);
            deletionFileNames.forEach(eventPublisher::publishFileDeletedEvent);

            eventPublisher.publishTechprocessUpdatedEvent(existingProcess);

            return repository.findById(existingProcess.getId()).get();
        });
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#fullNumber")
    public void sendToReview(String fullNumber) {
        var existing = getEditableProcess(fullNumber);
        checkTechprocessOwner(existing);

        var now = Instant.now();

        repository.sendToReview(existing.getId(), TechnologicalProcessStatus.IN_REVIEW, now, now);
        eventPublisher.publishProcessSentToReviewEvent(existing);
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#fullNumber")
    public void approve(String fullNumber) {
        var existing = repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(
                        fullNumber, TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесса с номером %s и статусом \"На проверке\" не найдено".formatted(fullNumber)));

        checkTechprocessReviewer(existing);

        repository.approve(existing.getId(), TechnologicalProcessStatus.SET_UP, Instant.now());
        eventPublisher.publishProcessSentToReviewEvent(existing);
    }

    @Transactional
    @CachePut(value = "techprocess", key = "#result.fullNumber")
    public TechnologicalProcess createRevision(String fullNumber) {
        var existing = repository.findByFullNumberAndStatus(fullNumber, TechnologicalProcessStatus.SET_UP)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесс с номером %s в статусе 'Наладочный' не найден".formatted(fullNumber)));

        checkTechprocessOwner(existing);

        var copyTechprocess = getCopySetUpTechprocess(existing);
        copyTechprocess.setStatus(TechnologicalProcessStatus.IN_CORRECTION);
        copyTechprocess.setRevision(existing.getRevision() + 1);

        return repository.save(copyTechprocess);
    }

    @Transactional
    public void addCommentToTechprocess(String processNumber, ReviewComment comment) {
        var existingProcess = repository.findByFullNumberAndStatus(processNumber, TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесс с номером %s в статусе 'на проверке' не найден".formatted(processNumber)));

        checkTechprocessReviewer(existingProcess);
        comment.setUuid(UUID.randomUUID());
        comment.setCreatedDate(Instant.now());

        repository.addCommentToTechprocess(existingProcess.getId(), comment);
    }

    @Transactional
    @CacheEvict(value = "techprocess", key = "#processNumber")
    public void addCommentToOperation(String processNumber, String operationNumber, ReviewComment comment) {
        var existingProcess = repository.findByFullNumberAndStatus(processNumber, TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесс с номером %s в статусе 'на проверке' не найден".formatted(processNumber)));

        checkTechprocessReviewer(existingProcess);
        getOperationByNumber(operationNumber, existingProcess);
        comment.setUuid(UUID.randomUUID());
        comment.setCreatedDate(Instant.now());

        repository.addCommentToOperation(existingProcess.getId(), operationNumber, comment);
    }

    @Transactional
    @CacheEvict(value = "techprocess", key = "#processNumber")
    public void returnForRevision(String processNumber) {
        var existing = repository.findByFullNumberAndStatus(processNumber, TechnologicalProcessStatus.IN_REVIEW)
                .orElseThrow(() -> new EntityNotFoundException(""));

        repository.setStatus(existing.getId(), TechnologicalProcessStatus.IN_CORRECTION);
        eventPublisher.publishTechprocessReturnedAfterReviewEvent(existing);
    }

    @Transactional
    @CacheEvict(value = "techprocess", key = "#processNumber")
    public void resolveComment(String processNumber, UUID commentId) {
        var existingTechprocess = repository.findByFullNumberAndStatus(processNumber, TechnologicalProcessStatus.IN_CORRECTION)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Техпроцесс с номером %s не найден".formatted(processNumber)));

        checkTechprocessOwner(existingTechprocess);

        var existingCommentInTechprocess = existingTechprocess.getReviewComments()
                .stream()
                .filter(c -> c.getUuid().equals(commentId))
                .findFirst();

        if (existingCommentInTechprocess.isPresent()) {
            repository.removeCommentFromTechprocess(existingTechprocess.getId(), commentId);
            return;
        }

        for (var operation : existingTechprocess.getOperations()) {
            var existingCommentInOperation = operation.getReviewComments()
                    .stream()
                    .filter(c -> c.getUuid().equals(commentId))
                    .findFirst();

            if (existingCommentInOperation.isPresent()) {
                repository.removeCommentFromOperation(existingTechprocess.getId(), operation.getNumber(), commentId);
                return;
            }
        }

        throw new EntityNotFoundException("Комментария с uuid %s нет в техпроцессе с номером %s"
                .formatted(commentId, processNumber));
    }

    private TechnologicalProcess getCopySetUpTechprocess(TechnologicalProcess original) {
        var copy = new TechnologicalProcess();
        copy.setPartNumber(original.getPartNumber());
        copy.setPartName(original.getPartName());
        copy.setRouteCardNote(original.getRouteCardNote());
        copy.setArchiveNumber(original.getArchiveNumber());
        copy.setDeveloperUsername(original.getDeveloperUsername());
        copy.setDeveloperFirstName(original.getDeveloperFirstName());
        copy.setDeveloperLastName(original.getDeveloperLastName());
        copy.setDeveloperFatherName(original.getDeveloperFatherName());
        copy.setReviewerUsername(original.getReviewerUsername());
        copy.setReviewerFirstName(original.getReviewerFirstName());
        copy.setReviewerLastName(original.getReviewerLastName());
        copy.setReviewerFatherName(original.getReviewerFatherName());
        copy.setWorkshopCode(original.getWorkshopCode());
        copy.setOrganizationType(original.getOrganizationType());
        copy.setWorkType(original.getWorkType());
        copy.setWorkName(original.getWorkName());
        copy.setFullNumber(original.getFullNumber());
        copy.setOperations(original.getOperations().stream().map(this::copyOperation).toList());

        return copy;
    }

    private TechnologicalOperation copyOperation(TechnologicalOperation o) {
        var copy = new TechnologicalOperation();
        copy.setNumber(o.getNumber());
        copy.setName(o.getName());
        copy.setIsOnlyForMan(o.getIsOnlyForMan());
        copy.setArea(o.getArea());
        copy.setWeight(o.getWeight());
        copy.setBlankType(o.getBlankType());
        copy.setEquipment(copyEquipment(o.getEquipment()));
        copy.setIsSertified(o.getIsSertified());
        copy.setOperationType(o.getOperationType());
        copy.setWorkerCodes(new ArrayList<>(o.getWorkerCodes()));

        if (!o.getSafetyInstructions().isEmpty()) {
            copy.setSafetyInstructions(o.getSafetyInstructions().stream()
                    .map(s -> new SafetyInstruction(s.getNumber(), s.getIsFromLibrary())).toList());
        }
        if (!o.getParts().isEmpty()) {
            copy.setParts(o.getParts().stream().map(this::copyPart).toList());
        }

        if (!o.getMaterials().isEmpty()) {
            copy.setMaterials(o.getMaterials().stream().map(this::copyMaterial).toList());
        }
        if (!o.getTransitions().isEmpty()) {
            copy.setTransitions(o.getTransitions().stream().map(this::copyTransition).toList());
        }

        if (!o.getSketches().isEmpty()) {
            copy.setSketches(o.getSketches().stream().map(this::copySketchCard).toList());
        }

        return copy;
    }

    private Equipment copyEquipment(Equipment e) {
        if (e == null) {
            return null;
        }

        var copy = new Equipment();
        copy.setName(e.getName());
        copy.setIndex(e.getIndex());
        copy.setStandard(e.getStandard());
        copy.setIsFromLibrary(e.getIsFromLibrary());

        return copy;
    }

    private Part copyPart(Part p) {
        var copy = new Part();
        copy.setName(p.getName());
        copy.setNumber(p.getNumber());
        copy.setPosition(p.getPosition());
        copy.setNote(p.getNote());
        copy.setQuantity(p.getQuantity());
        copy.setMaterialUnit(p.getMaterialUnit());
        copy.setSupplierCode(p.getSupplierCode());

        return copy;
    }

    private Material copyMaterial(Material m) {
        var copy = new Material();
        copy.setName(m.getName());
        copy.setNote(m.getNote());
        copy.setPosition(m.getPosition());
        copy.setSupplierCode(m.getSupplierCode());
        copy.setUnit(m.getUnit());
        copy.setConsumptionRate(m.getConsumptionRate());
        copy.setIsFromLibrary(m.getIsFromLibrary());
        copy.setRationingUnit(m.getRationingUnit());
        copy.setStandard(m.getStandard());

        return copy;
    }

    private TechnologicalTransition copyTransition(TechnologicalTransition t) {
        var copy = new TechnologicalTransition();
        copy.setNumber(t.getNumber());
        copy.setContent(t.getContent());

        if (!t.getEquipments().isEmpty()) {
            copy.setEquipments(t.getEquipments().stream().map(this::copyEquipment).toList());
        }

        return copy;
    }

    private Sketch copySketchCard(Sketch s) {
        var copy = new Sketch();
        copy.setFileName(s.getFileName());
        copy.setBlankType(s.getBlankType());
        copy.setSketchSheetNumber(s.getSketchSheetNumber());
        copy.setOperationNumbers(new ArrayList<>(s.getOperationNumbers()));

        return copy;
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
        if (repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumber(organizationType, workType, archiveNumber)) {
            throw new EntityAlreadyExistException(
                    "Technological process with organization type %s, work type %s, archive number %s existing"
                            .formatted(organizationType, workType, archiveNumber));
        }
    }

    private void checkIsNumberExistInWorkshop(String organizationType, String workType,
                                              String archiveNumber, String workshopCode) {
        if (repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(
                organizationType, workType, archiveNumber, workshopCode)) {
            throw new EntityAlreadyExistException(
                    "Technological process with organization type %s, work type %s, archive number %s, workshop code %s existing"
                            .formatted(organizationType, workType, archiveNumber, workshopCode));
        }
    }

    private void validateUniqueness(TechnologicalProcess techprocess) {
        checkIsNumberExist(techprocess.getOrganizationType().name(),
                techprocess.getWorkType().name(), techprocess.getArchiveNumber());
        checkIsNumberExistInWorkshop(techprocess.getOrganizationType().name(),
                techprocess.getWorkType().name(), techprocess.getArchiveNumber(), techprocess.getWorkshopCode());
    }

    private void checkSelfReview(TechnologicalProcess techprocess) {
        if (techprocess.getDeveloperUsername().equals(techprocess.getReviewerUsername())) {
            throw new TechprocessSavingException("Нельзя назначить на проверяющего самого себя");
        }
    }

    private void fillMetadata(TechnologicalProcess techprocess) {
        techprocess.setStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        techprocess.setRevision(0);
        techprocess.setFullNumber(fullNumberBuilder.buildFullNumber(
                techprocess.getOrganizationType(), techprocess.getWorkType(), techprocess.getArchiveNumber()));
    }

    private void fillDeveloper(TechnologicalProcess techprocess) {
        var username = jwtParser.getUsername();
        var firstName = jwtParser.getFirstName();
        var lastName = jwtParser.getLastName();
        var fatherName = jwtParser.getFatherName();

        if (username == null || username.isBlank() ||
                firstName == null || firstName.isBlank() ||
                lastName == null || lastName.isBlank()) {
            throw new TechprocessSavingException("Developer information is incomplete.");
        }

        techprocess.setDeveloperUsername(username);
        techprocess.setDeveloperFirstName(firstName);
        techprocess.setDeveloperLastName(lastName);
        techprocess.setDeveloperFatherName(fatherName);
    }

    private void checkTechprocessOwner(TechnologicalProcess tp) {
        if (!tp.getDeveloperUsername().equals(jwtParser.getUsername())) {
            throw new AuthorizationException("Нельзя выполнить операцию не являясь владельцем техпроцесса");
        }
    }

    private void checkTechprocessReviewer(TechnologicalProcess tp) {
        if (!tp.getReviewerUsername().equals(jwtParser.getUsername())) {
            throw new AuthorizationException("Нет прав для согласования техпроцесса");
        }
    }

    private void validateOperationNumbersUniqueness(List<TechnologicalOperation> operations) {
        var numbers = new HashSet<>();
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
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!duplicates.isEmpty()) {
            throw new EntityAlreadyExistException(
                    "Переходы с номерами %s повторяются в операции %s".formatted(duplicates, operation.getNumber()));
        }
    }

    private List<String> uploadSketchImages(List<MultipartFile> newSketchFiles) {
        if (newSketchFiles == null) {
            return List.of();
        }

        List<String> newFileNames = new ArrayList<>();
        for (var image : newSketchFiles) {
            newFileNames.add(fileServiceClient.uploadImage(image).fileName());
        }

        return newFileNames;
    }

    private void setUploadedFileNamesToSketches(List<String> uploadedFileNames, List<Sketch> sketches) {
        for (var sketchCard : sketches) {
            if (sketchCard.getFileName() == null) {
                sketchCard.setFileName(uploadedFileNames.getFirst());
                uploadedFileNames.removeFirst();
            }
        }
    }

    private List<String> getDeletionFileNames(TechnologicalOperation newOperation, TechnologicalOperation oldOperation) {
        var newFileNames = newOperation.getSketches()
                .stream()
                .map(Sketch::getFileName)
                .toList();

        return oldOperation.getSketches().stream()
                .map(Sketch::getFileName)
                .filter(fileName -> fileName != null && !newFileNames.contains(fileName))
                .toList();
    }

    private TechnologicalOperation getOperationByNumber(String operationNumber, TechnologicalProcess process) {
        return process.getOperations().stream()
                .filter(op -> op.getNumber().equals(operationNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Операция с номером %s не найдена в техпроцессе %s".formatted(operationNumber, process.getFullNumber())));
    }

    private void updateOperationInDatabase(String operationNumber, TechnologicalOperation newOperation,
                                           TechnologicalProcess existingProcess) {
        var query = Query.query(Criteria.where("_id").is(existingProcess.getId())
                .and("operations.number").is(operationNumber));
        var update = new Update().set("operations.$", newOperation);

        mongoTemplate.updateFirst(query, update, TechnologicalProcess.class);
    }
}
