package by.niruin.techprocess_service.unit;

import by.niruin.techprocess_service.client.FileServiceClient;
import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;
import by.niruin.techprocess_service.exception.*;
import by.niruin.techprocess_service.kafka.EventPublisher;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.security.JwtParser;
import by.niruin.techprocess_service.util.TechnologicalProcessFullNumberBuilder;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnologicalProcessServiceTest {
    @Mock
    private TechnologicalProcessRepository repository;
    @Mock
    private TechnologicalProcessFullNumberBuilder fullNumberBuilder;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private JwtParser jwtParser;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private FileServiceClient fileServiceClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private TechnologicalProcessService service;

    @BeforeEach
    void setUp() {
        lenient().when(jwtParser.getFirstName()).thenReturn("Евгений");
        lenient().when(jwtParser.getLastName()).thenReturn("Лагун");
        lenient().when(jwtParser.getFatherName()).thenReturn("Сергеевич");
        lenient().when(jwtParser.getUsername()).thenReturn("lagun123");
    }

    @Test
    void save_success() {
        var techprocess = createValidTechprocess();
        when(repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumber(any(), any(), any())).thenReturn(false);
        when(repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(any(), any(), any(), any()))
                .thenReturn(false);
        when(fullNumberBuilder.buildFullNumber(any(), any(), any())).thenReturn("100316761292.02188.12345");
        when(repository.save(any(TechnologicalProcess.class))).thenReturn(techprocess);

        var result = service.save(techprocess);

        assertThat(result).isNotNull();
        verify(repository).save(techprocess);
        verify(eventPublisher).publishTechprocessCreatedEvent(techprocess);
    }

    @Test
    void save_shouldThrowException_whenTechprocessAlreadyExists() {
        var techprocess = createValidTechprocess();
        when(repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumber(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.save(techprocess))
                .isInstanceOf(EntityAlreadyExistException.class);

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishTechprocessCreatedEvent(any());
    }

    @Test
    void save_shouldThrowException_whenSelfReview() {
        var techprocess = createValidTechprocess();
        techprocess.setReviewerUsername("lagun123");
        when(repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumber(any(), any(), any())).thenReturn(false);
        when(repository.existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(any(), any(), any(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.save(techprocess))
                .isInstanceOf(TechprocessSavingException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.SET_UP);
        when(repository.findFirstByFullNumberOrderByRevisionDesc(anyString())).thenReturn(Optional.of(techprocess));

        service.cancel("100316761292.02188.12345");

        verify(repository).setStatus(techprocess.getId(), TechnologicalProcessStatus.CANCELLED);
        verify(eventPublisher).publishTechprocessCancelledEvent(techprocess);
    }

    @Test
    void cancel_shouldThrowException_whenTechprocessNotFound() {
        when(repository.findFirstByFullNumberOrderByRevisionDesc(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel("12345"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).setStatus(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = TechnologicalProcessStatus.class, names = {"IN_DEVELOPMENT", "IN_CORRECTION", "IN_REVIEW", "PRODUCTION", "CANCELLED"})
    void cancel_shouldThrowException_whenStatusNotSetUp(TechnologicalProcessStatus status) {
        var techprocess = createTechprocessWithStatus(status);
        when(repository.findFirstByFullNumberOrderByRevisionDesc(anyString())).thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.cancel("12345"))
                .isInstanceOf(TechprocessCancellationException.class);

        verify(repository, never()).setStatus(any(), any());
    }

    @Test
    void cancel_shouldThrowException_whenNotOwner() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.SET_UP);
        techprocess.setDeveloperUsername("otherUser");
        when(repository.findFirstByFullNumberOrderByRevisionDesc(anyString())).thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.cancel("12345"))
                .isInstanceOf(AuthorizationException.class);

        verify(repository, never()).setStatus(any(), any());
    }

    @Test
    void getInStatusSetUpByNumber_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.SET_UP);
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.SET_UP)))
                .thenReturn(Optional.of(techprocess));

        var result = service.getInStatusSetUpByNumber("12345");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TechnologicalProcessStatus.SET_UP);
    }

    @Test
    void getInStatusSetUpByNumber_shouldThrowException_whenNotFound() {
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.SET_UP)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInStatusSetUpByNumber("12345"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getByNumberAndRevision_success() {
        var techprocess = createValidTechprocess();
        when(repository.findByFullNumberAndRevision(anyString(), anyInt())).thenReturn(Optional.of(techprocess));

        var result = service.getByNumberAndRevision("12345", 0);

        assertThat(result).isNotNull();
    }

    @Test
    void getByNumberAndRevision_shouldThrowException_whenNotFound() {
        when(repository.findByFullNumberAndRevision(anyString(), anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByNumberAndRevision("12345", 0))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getPageByStatus_shouldReturnPage() {
        var pageable = mock(Pageable.class);
        var techprocess = createValidTechprocess();
        var page = new PageImpl<>(List.of(techprocess));
        when(repository.findAllByStatus(eq(TechnologicalProcessStatus.IN_DEVELOPMENT), eq(pageable))).thenReturn(page);

        var result = service.getPageByStatus(TechnologicalProcessStatus.IN_DEVELOPMENT, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void update_success() {
        var existing = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        var newTechprocess = createValidTechprocess();
        newTechprocess.setPartNumber("NEW-123");
        newTechprocess.setWorkshopCode("005");

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), any()))
                .thenReturn(Optional.of(existing));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(TechnologicalProcess.class)))
                .thenReturn(null);
        when(repository.findById(anyString())).thenReturn(Optional.of(existing));

        var result = service.update("12345", newTechprocess);

        assertThat(result).isNotNull();
        verify(eventPublisher).publishTechprocessUpdatedEvent(existing);
    }

    @Test
    void update_shouldThrowException_whenProcessNotEditable() {
        var existing = createTechprocessWithStatus(TechnologicalProcessStatus.SET_UP);
        var newTechprocess = createValidTechprocess();

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), any()))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("12345", newTechprocess))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_shouldThrowException_whenChangingWorkshopInCorrection() {
        var existing = createTechprocessWithStatus(TechnologicalProcessStatus.IN_CORRECTION);
        existing.setWorkshopCode("001");
        var newTechprocess = createValidTechprocess();
        newTechprocess.setWorkshopCode("002");

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update("12345", newTechprocess))
                .isInstanceOf(TechprocessUpdatingException.class);
    }

    @Test
    void addOperation_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        var operation = createValidOperation();

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_DEVELOPMENT)))
                .thenReturn(Optional.of(techprocess));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(TechnologicalProcess.class)))
                .thenReturn(techprocess);

        var result = service.addOperation("12345", operation);

        assertThat(result).isNotNull();
    }

    @Test
    void addOperation_shouldThrowException_whenOperationNumberExists() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        var operation = createValidOperation();
        techprocess.setOperations(List.of(operation));

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_DEVELOPMENT)))
                .thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.addOperation("12345", operation))
                .isInstanceOf(EntityAlreadyExistException.class);
    }

    @Test
    void deleteOperation_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        var operation = createValidOperation();
        techprocess.setOperations(List.of(operation));

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_DEVELOPMENT)))
                .thenReturn(Optional.of(techprocess));

        service.deleteOperation("12345", "005");

        verify(repository).deleteOperation(techprocess.getId(), "005");
    }

    @Test
    void deleteOperation_shouldThrowException_whenOperationNotFound() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        techprocess.setOperations(List.of());

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_DEVELOPMENT)))
                .thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.deleteOperation("12345", "005"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void sendToReview_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.empty());
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_DEVELOPMENT)))
                .thenReturn(Optional.of(techprocess));

        service.sendToReview("12345");

        verify(repository).sendToReview(eq(techprocess.getId()), eq(TechnologicalProcessStatus.IN_REVIEW), any(Instant.class), any(Instant.class));
        verify(eventPublisher).publishProcessSentToReviewEvent(techprocess);
    }

    @Test
    void approve_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_REVIEW);
        techprocess.setReviewerUsername("ivanov123");

        when(jwtParser.getUsername()).thenReturn("ivanov123");
        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_REVIEW)))
                .thenReturn(Optional.of(techprocess));

        service.approve("12345");

        verify(repository).approve(techprocess.getId(), TechnologicalProcessStatus.SET_UP, Instant.now());
    }

    @Test
    void approve_shouldThrowException_whenNotReviewer() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_REVIEW);
        techprocess.setReviewerUsername("otherUser");

        when(repository.findFirstByFullNumberAndStatusOrderByRevisionDesc(anyString(), eq(TechnologicalProcessStatus.IN_REVIEW)))
                .thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.approve("12345"))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void createRevision_success() {
        var existing = createTechprocessWithStatus(TechnologicalProcessStatus.SET_UP);
        existing.setRevision(0);

        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.SET_UP)))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(TechnologicalProcess.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createRevision("12345");

        assertThat(result).isNotNull();
        assertThat(result.getRevision()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(TechnologicalProcessStatus.IN_CORRECTION);
    }

    @Test
    void addCommentToTechprocess_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_REVIEW);
        techprocess.setReviewerUsername("ivanov123");
        var comment = new ReviewComment();
        comment.setContent("Test comment");

        when(jwtParser.getUsername()).thenReturn("ivanov123");
        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.IN_REVIEW)))
                .thenReturn(Optional.of(techprocess));

        service.addCommentToTechprocess("12345", comment);

        assertThat(comment.getUuid()).isNotNull();
        verify(repository).addCommentToTechprocess(techprocess.getId(), comment);
    }

    @Test
    void returnForRevision_success() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_REVIEW);

        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.IN_REVIEW)))
                .thenReturn(Optional.of(techprocess));

        service.returnForRevision("12345");

        verify(repository).setStatus(techprocess.getId(), TechnologicalProcessStatus.IN_CORRECTION);
        verify(eventPublisher).publishTechprocessReturnedAfterReviewEvent(techprocess);
    }

    @Test
    void resolveComment_success_whenCommentInTechprocess() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_CORRECTION);
        var commentId = UUID.randomUUID();
        var comment = new ReviewComment();
        comment.setUuid(commentId);
        techprocess.setReviewComments(List.of(comment));

        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.of(techprocess));

        service.resolveComment("12345", commentId);

        verify(repository).removeCommentFromTechprocess(techprocess.getId(), commentId);
    }

    @Test
    void resolveComment_success_whenCommentInOperation() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_CORRECTION);
        var commentId = UUID.randomUUID();
        var comment = new ReviewComment();
        comment.setUuid(commentId);
        var operation = createValidOperation();
        operation.setReviewComments(List.of(comment));
        techprocess.setOperations(List.of(operation));

        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.of(techprocess));

        service.resolveComment("12345", commentId);

        verify(repository).removeCommentFromOperation(techprocess.getId(), operation.getNumber(), commentId);
    }

    @Test
    void resolveComment_shouldThrowException_whenCommentNotFound() {
        var techprocess = createTechprocessWithStatus(TechnologicalProcessStatus.IN_CORRECTION);

        when(repository.findByFullNumberAndStatus(anyString(), eq(TechnologicalProcessStatus.IN_CORRECTION)))
                .thenReturn(Optional.of(techprocess));

        assertThatThrownBy(() -> service.resolveComment("12345", UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private TechnologicalProcess createValidTechprocess() {
        var techprocess = new TechnologicalProcess();
        techprocess.setId(UUID.randomUUID().toString());
        techprocess.setPartNumber("ДЕТ-1234567");
        techprocess.setPartName("Вал-шестерня");
        techprocess.setArchiveNumber("12345");
        techprocess.setDeveloperUsername("lagun123");
        techprocess.setDeveloperFirstName("Евгений");
        techprocess.setDeveloperLastName("Лагун");
        techprocess.setDeveloperFatherName("Сергеевич");
        techprocess.setReviewerUsername("ivanov123");
        techprocess.setReviewerFirstName("Иванов");
        techprocess.setReviewerLastName("Иван");
        techprocess.setReviewerFatherName("Иванович");
        techprocess.setWorkshopCode("003");
        techprocess.setOrganizationType(TechnologicalProcessOrganizationType.SINGLE);
        techprocess.setWorkType(TechnologicalProcessWorkType.ASSEMBLY);
        techprocess.setWorkName("Сборки");
        techprocess.setRouteCardNote("Примечание");
        techprocess.setOperations(new ArrayList<>());
        techprocess.setReviewComments(new ArrayList<>());
        techprocess.setFullNumber("100316761292.02188.12345");
        techprocess.setRevision(0);
        techprocess.setStatus(TechnologicalProcessStatus.IN_DEVELOPMENT);
        return techprocess;
    }

    private TechnologicalProcess createTechprocessWithStatus(TechnologicalProcessStatus status) {
        var techprocess = createValidTechprocess();
        techprocess.setStatus(status);
        return techprocess;
    }

    private TechnologicalOperation createValidOperation() {
        var operation = new TechnologicalOperation();
        operation.setNumber("005");
        operation.setName("Сборка");
        operation.setWorkerCodes(List.of("01490"));
        operation.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        operation.setOperationType(OperationType.ASSEMBLY);
        operation.setArea(19);
        operation.setWeight(4.5);
        operation.setIsOnlyForMan(false);
        operation.setSafetyInstructions(new ArrayList<>());
        operation.setParts(new ArrayList<>());
        operation.setMaterials(new ArrayList<>());
        operation.setTransitions(new ArrayList<>());
        operation.setSketches(new ArrayList<>());
        operation.setReviewComments(new ArrayList<>());
        return operation;
    }
}
