package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.MaterialUnit;
import by.niruin.techprocess_service.domain.enums.OperationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalOperationMapper;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.technological_process.AddOperationRequest;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.technological_process.TechnologicalProcessDto;
import by.niruin.techprocess_service.model.technological_process.UpdateOperationRequest;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.security.JwtParser;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus.IN_DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TechnologicalProcessServiceIT extends BaseIntegrationTest {
    private static final Logger log = LogManager.getLogger(TechnologicalProcessServiceIT.class);

    @Container
    @ServiceConnection
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0")
            .withReplicaSet();

    @Autowired
    private TechnologicalProcessRepository technologicalProcessRepository;
    @Autowired
    private TransactionOutboxRepository transactionOutboxRepository;
    @Autowired
    private TechnologicalProcessMapper processMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtParser jwtParser;
    @Autowired
    private TechnologicalProcessService technologicalProcessService;
    @Autowired
    private TechnologicalOperationMapper technologicalOperationMapper;

    @BeforeEach
    void cleanDatabase() {
        technologicalProcessRepository.deleteAll();
        transactionOutboxRepository.deleteAll();
    }

    @Test
    void save_success() throws Exception {
        var request = createValidRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isCreated(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        jsonPath("$.id").exists(),
                        jsonPath("$.status").value(IN_DEVELOPMENT.name()),
                        jsonPath("$.revision").value(0),
                        jsonPath("$.fullNumber").value("100316761292.02188.12345"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.routeCardNote").exists(),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());

        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
        assertThat(transactionOutboxRepository.findAll()).hasSize(1);
    }

    @Test
    void save_shouldThrowsValidationException() throws Exception {
        var request = createInvalidRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(0);
        assertThat(transactionOutboxRepository.findAll()).hasSize(0);
    }

    @Test
    void save_shouldThrowsCreationException_whenHasSelfReviewer() throws Exception {
        var request = createRequestWithSelfReviewer();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(0);
        assertThat(transactionOutboxRepository.findAll()).hasSize(0);
    }

    @Test
    void save_shouldThrowsAlreadyExistException() throws Exception {
        createProcessInDb(
                IN_DEVELOPMENT,
                "Петр",
                "Петров",
                "Петрович");
        var request = createValidRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.CONFLICT.value()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
    }

    @Test
    void cancel_success() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get().getStatus())
                .isEqualTo(TechnologicalProcessStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowNotFoundException_whenProcessNotExist() {
        assertThat(technologicalProcessRepository.findByFullNumber("99999999")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TechnologicalProcessStatus.class,
            names = {"IN_DEVELOPMENT", "IN_CORRECTION", "IN_REVIEW", "PRODUCTION", "CANCELLED"})
    void cancel_shouldThrowConflict_whenStatusNotAllowed(TechnologicalProcessStatus status) throws Exception {
        var saved = createProcessInDb(status,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isConflict());
    }

    @Test
    void getByNumberAndRevision_shouldReturnRevisionZero() throws Exception {
        var request = createValidRequest();
        var response = mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isCreated())
                .andReturn();

        var savedDto = fromJson(response.getResponse().getContentAsString(), TechnologicalProcessDto.class);

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                        savedDto.fullNumber())
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpectAll(
                        jsonPath("$.id").value(savedDto.id()),
                        jsonPath("$.status").value(savedDto.status().name()),
                        jsonPath("$.revision").value(0),
                        jsonPath("$.fullNumber").value(savedDto.fullNumber()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.routeCardNote").exists(),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());
    }

    @Test
    void getByNumber_shouldThrowNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}", "41324312")
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void getByNumberAndRevision_shouldThrowNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                        "41324312")
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void getPageInStatusSetUp_shouldReturnPageWithOneTechprocess() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.content").isArray(),
                        jsonPath("$.content.length()").value(1),
                        jsonPath("$.content[0].id").value(saved.getId()),
                        jsonPath("$.content[0].archiveNumber").value(saved.getArchiveNumber()),
                        jsonPath("$.content[0].partName").value(saved.getPartName()),
                        jsonPath("$.content[0].status").value(saved.getStatus().name()),
                        jsonPath("$.content[0].fullNumber").value(saved.getFullNumber()),
                        jsonPath("$.content[0].workName").value(saved.getWorkName()),
                        jsonPath("$.totalElements").value(1),
                        jsonPath("$.totalPages").value(1));
    }

    @Test
    void sendToReviewAndApprove_success() throws Exception {
        var saved = createProcessInDb(
                IN_DEVELOPMENT,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                        saved.getFullNumber())
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                        saved.getFullNumber())
                        .with(jwtClaims("Иванов", "Иван", "Иванович")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber())
                .get().getStatus())
                .isEqualTo(TechnologicalProcessStatus.SET_UP);
    }

    @Test
    void sendToReview_throwNotFoundException() throws Exception {
        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                        "535432")
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isNotFound());
    }

    @Test
    void approve_throwNotFoundException() throws Exception {
        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                        "535432")
                        .with(jwtClaims("Петр", "Петров", "Петрович")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_success() throws Exception {
        var saved = createEditableProcessInDb();
        var request = createAddOperationRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(saved.getId()),
                        jsonPath("$.status").value("IN_DEVELOPMENT"),
                        jsonPath("$.operations.length()").value(1),
                        jsonPath("$.operations[0].number").value(request.number()),
                        jsonPath("$.operations[0].name").value(request.name()));
    }

    @Test
    void addOperation_successFullValidation() throws Exception {
        var saved = createEditableProcessInDb();
        var request = createAddOperationRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(saved.getId()),
                        jsonPath("$.operations.length()").value(1),
                        jsonPath("$.operations[0].number").value(request.number()),
                        jsonPath("$.operations[0].name").value(request.name()),
                        jsonPath("$.operations[0].workerCodes[0]")
                                .value(request.workerCodes().getFirst()),
                        jsonPath("$.operations[0].blankType")
                                .value(request.blankType().name()),
                        jsonPath("$.operations[0].equipment.name")
                                .value(request.equipmentReference().getName()),
                        jsonPath("$.operations[0].parts.length()")
                                .value(request.partReferences().size()),
                        jsonPath("$.operations[0].parts[0].name")
                                .value(request.partReferences().getFirst().getName()),
                        jsonPath("$.operations[0].materials.length()")
                                .value(request.materialReferences().size()),
                        jsonPath("$.operations[0].materials[0].name")
                                .value(request.materialReferences().getFirst().getName()),
                        jsonPath("$.operations[0].safetyInstructions.length()")
                                .value(request.safetyInstructionReferences().size()),
                        jsonPath("$.operations[0].transitions.length()").value(0));
    }

    @Test
    void addOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_shouldThrowNotFound_whenStatusNotEditable() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_shouldThrowAuthorizationException() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_CORRECTION,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addOperation_shouldThrowConflict_whenOperationNumberExists() throws Exception {
        var saved = createEditableProcessInDb();
        var operation = technologicalOperationMapper.toOperation(getAddOperationRequest());
        saved.addOperation(operation);
        technologicalProcessRepository.save(saved);
        var duplicateOperation = createAddOperationRequest();

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicateOperation))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOperation_success() throws Exception {
        var saved = createProcessWithOperationInDb();
        var operation = saved.getOperations().getFirst();

        mockMvc.perform(delete(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), operation.getNumber())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        "99999", "005")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOperation_shouldThrowNotFound_whenOperationNotExist() throws Exception {
        var saved = createEditableProcessInDb();

        mockMvc.perform(delete(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), "999")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOperation_shouldThrowAuthorizationException() throws Exception {
        var techprocess = createProcessInDb(
                IN_DEVELOPMENT,
                "Петр",
                "Петров",
                "Петрович");
        var operation = technologicalOperationMapper.toOperation(getAddOperationRequest());
        techprocess.addOperation(operation);
        var saved = technologicalProcessRepository.save(techprocess);

        mockMvc.perform(delete(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), operation.getNumber())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOperation_success() throws Exception {
        var saved = createProcessWithOperationInDb();
        var operation = saved.getOperations().getFirst();
        var updateRequest = createUpdateOperationRequest();

        mockMvc.perform(put(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), operation.getNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(status().isOk());
    }

    @Test
    void updateOperation_shouldThrowAuthorizationException() throws Exception {
        var techprocess = createProcessInDb(
                IN_DEVELOPMENT,
                "Петр",
                "Петров",
                "Петрович");
        var operation = technologicalOperationMapper.toOperation(getAddOperationRequest());
        techprocess.addOperation(operation);
        var saved = technologicalProcessRepository.save(techprocess);

        mockMvc.perform(put(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), operation.getNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createUpdateOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(put(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        "99999", "005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createUpdateOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOperation_shouldThrowNotFound_whenOperationNotExist() throws Exception {
        var saved = createEditableProcessInDb();

        mockMvc.perform(put(
                        "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                        saved.getFullNumber(), "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createUpdateOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_success() throws Exception {
        var saved = createEditableProcessInDb();
        var updateRequest = createValidRequest();

        mockMvc.perform(put("/api/v1/techprocess-service/technological-processes/{full-number}",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpectAll(status().isOk(),
                        jsonPath("$.id").value(saved.getId()),
                        jsonPath("$.partNumber").value(updateRequest.partNumber()),
                        jsonPath("$.partName").value(updateRequest.partName()),
                        jsonPath("$.archiveNumber").value(updateRequest.archiveNumber()),
                        jsonPath("$.reviewerFirstName").value(updateRequest.reviewerFirstName()),
                        jsonPath("$.reviewerLastName").value(updateRequest.reviewerLastName()),
                        jsonPath("$.reviewerFatherName").value(updateRequest.reviewerFatherName()),
                        jsonPath("$.workshopCode").value(updateRequest.workshopCode()),
                        jsonPath("$.workType").value(updateRequest.workType()),
                        jsonPath("$.organizationType").value(updateRequest.organizationType()),
                        jsonPath("$.workName").value(updateRequest.workName()),
                        jsonPath("$.routeCardNote").value(updateRequest.routeCardNote()),
                        jsonPath("$.developerFirstName").value(saved.getDeveloperFirstName()),
                        jsonPath("$.developerLastName").value(saved.getDeveloperLastName()),
                        jsonPath("$.developerFatherName").value(saved.getDeveloperFatherName()),
                        jsonPath("$.status").value(saved.getStatus().name()),
                        jsonPath("$.revision").value(saved.getRevision()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists());
    }

    @Test
    void update_shouldThrowException_whenChangeWorkshopInCorrection() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_CORRECTION,
                "Евгений",
                "Лагун",
                "Сергеевич");
        var request = new CreateTechprocessRequest(
                "ДЕТ-1234567",
                "Вал-шестерня",
                "12345",
                "Иванов",
                "Иван",
                "Иванович",
                "004",
                "ASSEMBLY",
                "SINGLE",
                "Сборки",
                "Примечание");

        mockMvc.perform(put("/api/v1/techprocess-service/technological-processes/{full-number}",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldThrowException_whenStatusNotEditable() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(put("/api/v1/techprocess-service/technological-processes/{full-number}",
                        saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createValidRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич")))
                .andExpect(status().isNotFound());
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtClaims(String firstName,
                                                                                   String lastName,
                                                                                   String fatherName) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> {
                    jwt.claim("first_name", firstName);
                    jwt.claim("last_name", lastName);
                    jwt.claim("father_name", fatherName);
                });
    }

    private TechnologicalProcess createProcessInDb(TechnologicalProcessStatus status,
                                                   String devFirstName,
                                                   String devLastName,
                                                   String devFatherName) {
        var techprocess = processMapper.toTechnologicalProcess(createValidRequest());
        techprocess.setStatus(status);
        techprocess.setFullNumber("100316761292.02188.12345");
        techprocess.setDeveloperFirstName(devFirstName);
        techprocess.setDeveloperLastName(devLastName);
        techprocess.setDeveloperFatherName(devFatherName);
        return technologicalProcessRepository.save(techprocess);
    }

    private TechnologicalProcess createEditableProcessInDb() {
        return createProcessInDb(
                IN_DEVELOPMENT,
                "Евгений",
                "Лагун",
                "Сергеевич");
    }

    private TechnologicalProcess createProcessWithOperationInDb() {
        var techprocess = createEditableProcessInDb();
        var op = technologicalOperationMapper.toOperation(getAddOperationRequest());
        techprocess.addOperation(op);
        return technologicalProcessRepository.save(techprocess);
    }

    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    private CreateTechprocessRequest createValidRequest() {
        return new CreateTechprocessRequest(
                "ДЕТ-1234567",
                "Вал-шестерня",
                "12345",
                "Иванов",
                "Иван",
                "Иванович",
                "003",
                "ASSEMBLY",
                "SINGLE",
                "Сборки",
                "Примечание");
    }

    private CreateTechprocessRequest createRequestWithSelfReviewer() {
        return new CreateTechprocessRequest(
                "ДЕТ-1234567",
                "Вал-шестерня",
                "12345",
                jwtParser.getLastName(),
                jwtParser.getFirstName(),
                jwtParser.getFatherName(),
                "003",
                "ASSEMBLY",
                "SINGLE",
                "Сборки",
                "Примечание");
    }

    private CreateTechprocessRequest createInvalidRequest() {
        return new CreateTechprocessRequest(
                "ДЕТ-1234567",
                "AD!@31",
                "fdsa",
                "Ivanov",
                "ivan",
                "Iv432",
                "abc",
                "TEST",
                "TESTT",
                "1239fdsa",
                "Примечание");
    }

    private AddOperationRequest getAddOperationRequest() {
        var partReference = PartReference.builder()
                .name("Вал")
                .number("2022-1312321-Б")
                .position("1")
                .quantity(1)
                .materialUnit(MaterialUnit.PIECE)
                .supplierCode("342")
                .build();

        var materialReference = MaterialReference.builder()
                .name("Литол-24")
                .standard("ГОСТ 2711-2017")
                .note("Тестовое описание")
                .unit(MaterialUnit.KILOGRAM)
                .isFromLibrary(true)
                .supplierCode("384")
                .position("10")
                .rationingUnit(1)
                .consumptionRate(0.005)
                .build();

        var equipmentReference = EquipmentReference.builder()
                .index("Б517")
                .isFromLibrary(false)
                .name("Стол-верстак")
                .build();

        return new AddOperationRequest(
                "005",
                "Сборочная",
                List.of("12345"),
                List.of(new SafetyInstructionReference("101", false)),
                List.of(partReference),
                List.of(materialReference),
                equipmentReference,
                "4",
                BlankType.OPERATION_BLANK_TITLE,
                1.5,
                1,
                false,
                OperationType.ASSEMBLY);
    }

    private AddOperationRequest createAddOperationRequest() {
        var partReference = PartReference.builder()
                .name("Вал")
                .position("1*")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE)
                .quantity(1)
                .supplierCode("342")
                .build();

        var materialReference = MaterialReference.builder()
                .name("Литол-24")
                .standard("ГОСТ 2711-2017")
                .note("Тестовое описание")
                .unit(MaterialUnit.KILOGRAM)
                .isFromLibrary(true)
                .supplierCode("384")
                .position("10")
                .rationingUnit(1)
                .consumptionRate(0.005)
                .build();

        var equipmentReference = EquipmentReference.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        return new AddOperationRequest(
                "005",
                "Сборка",
                List.of("01490"),
                List.of(new SafetyInstructionReference("75", false)),
                List.of(partReference),
                List.of(materialReference),
                equipmentReference,
                "4",
                BlankType.OPERATION_BLANK_TITLE,
                4.5,
                19,
                false,
                OperationType.ASSEMBLY);
    }

    private UpdateOperationRequest createUpdateOperationRequest() {
        var partReference = PartReference.builder()
                .name("Кронштейн")
                .position("3")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE)
                .quantity(2)
                .supplierCode("385")
                .build();

        var equipmentReference = EquipmentReference.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        var materialReference = MaterialReference.builder()
                .name("Масло И-20А")
                .standard("ГОСТ 2712-2022")
                .note("Тестовое описание")
                .unit(MaterialUnit.LITER)
                .isFromLibrary(true)
                .supplierCode("322")
                .position("9")
                .rationingUnit(1)
                .consumptionRate(0.002)
                .build();

        var transition = new TechnologicalTransition();
        transition.setNumber(1);
        transition.setContent("Взять кронштейн и смазать маслом");

        return new UpdateOperationRequest(
                "001",
                "Слесарная",
                List.of("14092"),
                List.of(new SafetyInstructionReference("100", false)),
                List.of(partReference),
                equipmentReference,
                "4",
                BlankType.OPERATION_BLANK_TITLE,
                10.0,
                1,
                true,
                OperationType.ASSEMBLY,
                List.of(materialReference),
                List.of(transition),
                List.of());
    }
}
