package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.domain.*;
import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.MaterialUnit;
import by.niruin.techprocess_service.domain.enums.OperationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalOperationMapper;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.file.UploadFileResponse;
import by.niruin.techprocess_service.model.technological_process.*;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.security.JwtParser;
import by.niruin.techprocess_service.service.TechnologicalProcessService;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus.IN_DEVELOPMENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WireMockTest
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
    @Autowired
    private MongoTemplate mongoTemplate;

    @RegisterExtension
    static WireMockExtension fileServiceWireMock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideFeignProperties(DynamicPropertyRegistry registry) {
        registry.add("file-service.url", fileServiceWireMock::baseUrl);
    }

    @BeforeEach
    void cleanDatabase() {
        technologicalProcessRepository.deleteAll();
        transactionOutboxRepository.deleteAll();
        fileServiceWireMock.resetAll();
    }

    @Test
    void save_success() throws Exception {
        var request = createValidRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get().getStatus())
                .isEqualTo(TechnologicalProcessStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowNotFoundException_whenProcessNotExist() throws Exception {
        var fullNumber = "030213123";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                                fullNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = TechnologicalProcessStatus.class,
            names = {"IN_DEVELOPMENT", "IN_CORRECTION", "IN_REVIEW", "PRODUCTION", "CANCELLED"})
    void cancel_shouldThrowConflict_whenStatusNotAllowed(TechnologicalProcessStatus status) throws Exception {
        var saved = createProcessInDb(status,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isConflict());
    }

    @Test
    void getByNumberAndRevision_shouldReturnRevisionZero() throws Exception {
        var request = createValidRequest();
        var response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isCreated())
                .andReturn();

        var savedDto = fromJson(response.getResponse().getContentAsString(), TechnologicalProcessDto.class);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                                savedDto.fullNumber())
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpectAll(
                        jsonPath("$.id").value(savedDto.id()),
                        jsonPath("$.status").value(savedDto.status().name()),
                        jsonPath("$.revision").value(0),
                        jsonPath("$.fullNumber").value(savedDto.fullNumber()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.routeCardNote").exists(),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());

        assertThat(technologicalProcessRepository.findByFullNumber(savedDto.fullNumber()).get().getRevision()).isEqualTo(0);
    }

    @Test
    void getByNumber_shouldThrowNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/techprocess-service/technological-processes/{full-number}", "41324312")
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void getByNumberAndRevision_shouldThrowNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                                "41324312")
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/techprocess-service/technological-processes")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
    }

    @Test
    void sendToReviewAndApprove_success() throws Exception {
        var saved = createProcessInDb(
                IN_DEVELOPMENT,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                                saved.getFullNumber())
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                                saved.getFullNumber())
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get().getStatus())
                .isEqualTo(TechnologicalProcessStatus.SET_UP);
    }

    @Test
    void sendToReview_throwNotFoundException() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                                "535432")
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void approve_throwNotFoundException() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                                "535432")
                        .with(jwtClaims("Петр", "Петров", "Петрович", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_success() throws Exception {
        var saved = createEditableProcessInDb();
        var request = createAddOperationRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(saved.getId()),
                        jsonPath("$.status").value("IN_DEVELOPMENT"),
                        jsonPath("$.operations.length()").value(1),
                        jsonPath("$.operations[0].number").value(request.number()),
                        jsonPath("$.operations[0].name").value(request.name()));

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get().getOperations())
                .hasSize(1);
    }

    @Test
    void addOperation_successFullValidation() throws Exception {
        var saved = createEditableProcessInDb();
        var request = createAddOperationRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        assertThat(technologicalProcessRepository.findAll().get(0).getOperations()).hasSize(1);
        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0).getNumber())
                .isEqualTo(request.number());

    }

    @Test
    void addOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                "99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_shouldThrowNotFound_whenStatusNotEditable() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_shouldThrowAuthorizationException() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_CORRECTION,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createAddOperationRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "fail")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addOperation_shouldThrowConflict_whenOperationNumberExists() throws Exception {
        var saved = createEditableProcessInDb();
        var operation = technologicalOperationMapper.toOperation(getAddOperationRequest());
        saved.addOperation(operation);
        technologicalProcessRepository.save(saved);
        var duplicateOperation = createAddOperationRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/operations",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicateOperation))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOperation_success() throws Exception {
        var saved = createProcessWithOperationInDb();
        var operation = saved.getOperations().getFirst();

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                                saved.getFullNumber(), operation.getNumber())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                                "99999", "005")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOperation_shouldThrowNotFound_whenOperationNotExist() throws Exception {
        var saved = createEditableProcessInDb();

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                                saved.getFullNumber(), "999")
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        mockMvc.perform(MockMvcRequestBuilders.delete(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                                saved.getFullNumber(), operation.getNumber())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "fail")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOperation_success() throws Exception {
        var saved = createProcessWithOperationInDb();
        var operation = saved.getOperations().getFirst();

        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0)).isNotNull();

        var updateRequest = createUpdateOperationRequest();

        mockMvc.perform(updateOperationRequest(saved.getFullNumber(), operation.getNumber(), updateRequest)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0).getName())
                .isEqualTo(updateRequest.name());
        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0).getNumber())
                .isEqualTo(updateRequest.number());
        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0).getArea())
                .isEqualTo(updateRequest.area());
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

        mockMvc.perform(updateOperationRequest(saved.getFullNumber(), operation.getNumber(), createUpdateOperationRequest())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "fail")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        mockMvc.perform(updateOperationRequest("99999", "005", createUpdateOperationRequest())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOperation_shouldThrowNotFound_whenOperationNotExist() throws Exception {
        var saved = createEditableProcessInDb();

        mockMvc.perform(updateOperationRequest(saved.getFullNumber(), "999", createUpdateOperationRequest())
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateOperation_addNewSketches_success() throws Exception {
        var savedProcess = createProcessWithOperationInDb();
        var existingOperation = savedProcess.getOperations().get(0);

        var newFileName1 = UUID.randomUUID() + ".png";
        var newFileName2 = UUID.randomUUID() + ".png";

        var newFile1 = new MockMultipartFile(
                "newSketchFiles",
                "new-sketch-1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "content1".getBytes());

        var newFile2 = new MockMultipartFile(
                "newSketchFiles",
                "new-sketch-2.png",
                MediaType.IMAGE_PNG_VALUE,
                "content2".getBytes());

        fileServiceWireMock.stubFor(post(urlEqualTo("/api/v1/file-service/files/images"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new UploadFileResponse(newFileName1)))));

        fileServiceWireMock.stubFor(post(urlEqualTo("/api/v1/file-service/files/images"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new UploadFileResponse(newFileName2)))));

        var request = createUpdateOperationRequestWithNewSketches(List.of(newFile1, newFile2));

        mockMvc.perform(updateOperationRequest(savedProcess.getFullNumber(), existingOperation.getNumber(), request)
                        .file(newFile1)
                        .file(newFile2)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findAll().get(0).getOperations().get(0).getSketchCards())
                .hasSize(2);
    }

    @Test
    void updateOperation_keepExistingSketches_success() throws Exception {
        var savedProcess = createProcessWithOperationInDb();
        var existingOperation = savedProcess.getOperations().get(0);

        var existingFileName1 = "existing-sketch-1.png";
        var existingFileName2 = "existing-sketch-2.png";

        var existingSketch1 = new SketchCard();
        existingSketch1.setFileName(existingFileName1);
        existingSketch1.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        existingSketch1.setSketchSheetNumber(1);

        var existingSketch2 = new SketchCard();
        existingSketch2.setFileName(existingFileName2);
        existingSketch2.setBlankType(BlankType.OPERATION_BLANK_CONTINUATION);
        existingSketch2.setSketchSheetNumber(2);

        existingOperation.setSketchCards(List.of(existingSketch1, existingSketch2));
        technologicalProcessRepository.save(savedProcess);

        var request = createUpdateOperationRequestWithExistingSketches();

        mockMvc.perform(updateOperationRequest(savedProcess.getFullNumber(), existingOperation.getNumber(), request)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        assertThat(transactionOutboxRepository.findAll()).hasSize(1);
    }

    @Test
    void updateOperation_mixedSketches_success() throws Exception {
        var savedProcess = createProcessWithOperationInDb();
        var existingOperation = savedProcess.getOperations().get(0);

        var existingFileName = "existing-sketch.png";
        SketchCard existingSketch = new SketchCard();
        existingSketch.setFileName(existingFileName);
        existingSketch.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        existingSketch.setSketchSheetNumber(1);

        existingOperation.setSketchCards(List.of(existingSketch));
        technologicalProcessRepository.save(savedProcess);

        var newFileName1 = UUID.randomUUID() + ".png";
        var newFileName2 = UUID.randomUUID() + ".png";

        var newFile1 = new MockMultipartFile(
                "newSketchFiles",
                "new-sketch-1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "content1".getBytes());

        var newFile2 = new MockMultipartFile(
                "newSketchFiles",
                "new-sketch-2.png",
                MediaType.IMAGE_PNG_VALUE,
                "content2".getBytes());

        fileServiceWireMock.stubFor(post(urlEqualTo("/api/v1/file-service/files/images"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new UploadFileResponse(newFileName1)))));

        fileServiceWireMock.stubFor(post(urlEqualTo("/api/v1/file-service/files/images"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.CREATED.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(new UploadFileResponse(newFileName2)))));

        var request = createUpdateOperationRequestWithMixedSketches(List.of(newFile1, newFile2));

        mockMvc.perform(updateOperationRequest(savedProcess.getFullNumber(), existingOperation.getNumber(), request)
                        .file(newFile1)
                        .file(newFile2)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        var updatedProcess = technologicalProcessRepository.findById(savedProcess.getId()).get();
        var updatedOperation = updatedProcess.getOperations().get(0);

        assertThat(updatedOperation.getSketchCards()).hasSize(3);
        assertThat(updatedOperation.getSketchCards().get(0).getFileName()).isEqualTo(existingFileName);
    }

    @Test
    void update_success() throws Exception {
        var saved = createEditableProcessInDb();
        var updateRequest = createValidRequest();

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/techprocess-service/technological-processes/{full-number}",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
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

        assertThat(technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get().getPartNumber())
                .isEqualTo(updateRequest.partNumber());
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
                "lagun123",
                "Иванов",
                "Иван",
                "Иванович",
                "004",
                "ASSEMBLY",
                "SINGLE",
                "Сборки",
                "Примечание");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/techprocess-service/technological-processes/{full-number}",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldThrowException_whenStatusNotEditable() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/techprocess-service/technological-processes/{full-number}",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createValidRequest()))
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRevision_success() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/create-revision",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").exists(),
                        jsonPath("$.status").value("IN_CORRECTION"),
                        jsonPath("$.revision").value(saved.getRevision() + 1),
                        jsonPath("$.fullNumber").value(saved.getFullNumber()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(2);
    }

    @Test
    void createRevision_shouldThrowNotFoundException_whenProcessNotSetUp() throws Exception {
        var saved = createProcessInDb(
                IN_DEVELOPMENT,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/create-revision",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRevision_shouldThrowAuthorizationException() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.SET_UP,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/techprocess-service/technological-processes/{full-number}/create-revision",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "invalid")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCommentToOperation_success() throws Exception {
        var saved = createProcessWithOperationInDb();
        saved.setStatus(TechnologicalProcessStatus.IN_REVIEW);
        technologicalProcessRepository.save(saved);

        var operation = saved.getOperations().getFirst();
        var request = new AddCommentRequest("Замечание к операции");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}/review-comments",
                                saved.getFullNumber(), operation.getNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isOk());

        var updated = technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get();

        var updatedOperation = updated.getOperations().stream()
                .filter(op -> op.getNumber().equals(operation.getNumber()))
                .findFirst().get();

        assertThat(updatedOperation.getReviewComments()).hasSize(1);
        assertThat(updatedOperation.getReviewComments().get(0).getContent()).isEqualTo("Замечание к операции");
    }

    @Test
    void addCommentToTechprocess_shouldThrowNotFoundException() throws Exception {
        var request = new AddCommentRequest("Замечание");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/review-comments",
                                "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCommentToTechprocess_shouldThrowAuthorizationException() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Петр",
                "Петров",
                "Петрович");

        var request = new AddCommentRequest("Замечание");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/review-comments",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Петр", "Петров", "Петрович", "petrov123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addCommentToTechprocess_success() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Петр",
                "Петров",
                "Петрович");

        var request = new AddCommentRequest("Замечание к техпроцессу");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/review-comments",
                                saved.getFullNumber()) // Используем динамический номер сохраненного процесса
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isOk());
        Thread.sleep(1000);
        var updatedProcess = mongoTemplate.findOne(
                Query.query(Criteria.where("fullNumber").is(saved.getFullNumber())),
                TechnologicalProcess.class
        );

        assertThat(updatedProcess).isNotNull();
        assertThat(updatedProcess.getReviewComments()).hasSize(1);

        assertThat(updatedProcess.getReviewComments().get(0).getContent())
                .isEqualTo("Замечание к техпроцессу");

        assertThat(updatedProcess.getReviewComments().get(0).getUuid())
                .isNotNull();
    }

    @Test
    void addCommentToOperation_shouldThrowNotFound_whenTechprocessNotExist() throws Exception {
        var request = new AddCommentRequest("Замечание");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}/review-comments",
                                "99999", "005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCommentToOperation_shouldThrowNotFound_whenOperationNotExist() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Петр",
                "Петров",
                "Петрович");

        var request = new AddCommentRequest("Замечание");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}/review-comments",
                                saved.getFullNumber(), "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCommentToOperation_shouldThrowAuthorizationException() throws Exception {
        var saved = createProcessWithOperationInDb();
        saved.setStatus(TechnologicalProcessStatus.IN_REVIEW);
        technologicalProcessRepository.save(saved);

        var operation = saved.getOperations().getFirst();
        var request = new AddCommentRequest("Замечание");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}/review-comments",
                                saved.getFullNumber(), operation.getNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
                        .with(jwtClaims("Петр", "Петров", "Петрович", "petrov123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnForRevision_success() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_REVIEW,
                "Петр",
                "Петров",
                "Петрович");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/return-for-revision",
                                saved.getFullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isOk());

        var updated = technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get();
        assertThat(updated.getStatus()).isEqualTo(TechnologicalProcessStatus.IN_CORRECTION);
    }

    @Test
    void returnForRevision_shouldThrowNotFoundException() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/return-for-revision",
                                "99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Иванов", "Иван", "Иванович", "ivanov123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void resolveComment_success_whenCommentInTechprocess() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_CORRECTION,
                "Евгений",
                "Лагун",
                "Сергеевич");

        var commentId = UUID.randomUUID();
        var comment = new ReviewComment();
        comment.setUuid(commentId);
        comment.setContent("Замечание");
        saved.addReviewComment(comment);
        technologicalProcessRepository.save(saved);

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/comments/{comment-id}/resolve",
                                saved.getFullNumber(), commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        var updated = technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get();
        assertThat(updated.getReviewComments()).isEmpty();
    }

    @Test
    void resolveComment_success_whenCommentInOperation() throws Exception {
        var saved = createProcessWithOperationInDb();
        saved.setStatus(TechnologicalProcessStatus.IN_CORRECTION);
        var operation = saved.getOperations().getFirst();

        var commentId = UUID.randomUUID();
        var comment = new ReviewComment();
        comment.setUuid(commentId);
        comment.setContent("Замечание");
        operation.addReviewComment(comment);
        technologicalProcessRepository.save(saved);

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/comments/{comment-id}/resolve",
                                saved.getFullNumber(), commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        var updated = technologicalProcessRepository.findByFullNumber(saved.getFullNumber()).get();
        var updatedOperation = updated.getOperations().getFirst();
        assertThat(updatedOperation.getReviewComments()).isEmpty();
    }

    @Test
    void resolveComment_shouldThrowNotFoundException() throws Exception {
        var saved = createProcessInDb(
                TechnologicalProcessStatus.IN_CORRECTION,
                "Евгений",
                "Лагун",
                "Сергеевич");

        mockMvc.perform(MockMvcRequestBuilders.post(
                                "/api/v1/techprocess-service/technological-processes/{full-number}/comments/{comment-id}/resolve",
                                saved.getFullNumber(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor jwtClaims(String firstName,
                                           String lastName,
                                           String fatherName,
                                           String username) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> {
                    jwt.claim("username", username);
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
        techprocess.setReviewerUsername("ivanov123");
        techprocess.setRevision(0);
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
                "lagun123",
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
                jwtParser.getUsername(),
                jwtParser.getFirstName(),
                jwtParser.getLastName(),
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
                "lagun123",
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
                List.of(),
                List.of());
    }

    private UpdateOperationRequest createUpdateOperationRequestWithSketches(List<SketchCard> sketches,
                                                                            List<MultipartFile> newFiles) {
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
                sketches,
                newFiles
        );
    }

    private UpdateOperationRequest createUpdateOperationRequestWithNewSketches(List<MultipartFile> newFiles) {
        var sketch1 = new SketchCard();
        sketch1.setFileName(null);
        sketch1.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        sketch1.setSketchSheetNumber(1);
        sketch1.setOperationNumbers(List.of(1, 2));

        var sketch2 = new SketchCard();
        sketch2.setFileName(null);
        sketch2.setBlankType(BlankType.OPERATION_BLANK_CONTINUATION);
        sketch2.setSketchSheetNumber(2);
        sketch2.setOperationNumbers(List.of(3, 4));

        return createUpdateOperationRequestWithSketches(List.of(sketch1, sketch2), newFiles);
    }

    private UpdateOperationRequest createUpdateOperationRequestWithExistingSketches() {
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

        var sketch1 = new SketchCard();
        sketch1.setFileName("existing-sketch-1.png");
        sketch1.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        sketch1.setSketchSheetNumber(1);
        sketch1.setOperationNumbers(List.of(1, 2));

        var sketch2 = new SketchCard();
        sketch2.setFileName("existing-sketch-2.png");
        sketch2.setBlankType(BlankType.OPERATION_BLANK_CONTINUATION);
        sketch2.setSketchSheetNumber(2);
        sketch2.setOperationNumbers(List.of(3, 4));

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
                List.of(sketch1, sketch2),
                List.of()
        );
    }

    private UpdateOperationRequest createUpdateOperationRequestWithMixedSketches(List<MultipartFile> newFiles) {
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

        var existingSketch = new SketchCard();
        existingSketch.setFileName("existing-sketch.png");
        existingSketch.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        existingSketch.setSketchSheetNumber(1);
        existingSketch.setOperationNumbers(List.of(1));

        var newSketch1 = new SketchCard();
        newSketch1.setFileName(null);
        newSketch1.setBlankType(BlankType.OPERATION_BLANK_CONTINUATION);
        newSketch1.setSketchSheetNumber(2);
        newSketch1.setOperationNumbers(List.of(2, 3));

        var newSketch2 = new SketchCard();
        newSketch2.setFileName(null);
        newSketch2.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        newSketch2.setSketchSheetNumber(3);
        newSketch2.setOperationNumbers(List.of(4));

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
                List.of(existingSketch, newSketch1, newSketch2),
                newFiles
        );
    }

    private MockMultipartHttpServletRequestBuilder updateOperationRequest(String fullNumber, String operationNumber,
                                                                          UpdateOperationRequest request) throws Exception {
        var builder = multipart(HttpMethod.PUT,
                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                fullNumber, operationNumber);

        builder.param("number", request.number());
        builder.param("name", request.name());
        builder.param("blankType", request.blankType().name());
        builder.param("operationType", request.operationType().name());
        builder.param("isOnlyForMan", String.valueOf(request.isOnlyForMan()));

        if (request.workerCategory() != null) {
            builder.param("workerCategory", request.workerCategory());
        }
        if (request.weight() != null) {
            builder.param("weight", String.valueOf(request.weight()));
        }
        if (request.area() != null) {
            builder.param("area", String.valueOf(request.area()));
        }

        if (request.workerCodes() != null) {
            for (int i = 0; i < request.workerCodes().size(); i++) {
                builder.param("workerCodes[" + i + "]", request.workerCodes().get(i));
            }
        }

        if (request.safetyInstructionReferences() != null) {
            for (int i = 0; i < request.safetyInstructionReferences().size(); i++) {
                builder.param("safetyInstructionReferences[" + i + "].number",
                        request.safetyInstructionReferences().get(i).getNumber());
                builder.param("safetyInstructionReferences[" + i + "].fromLibrary",
                        String.valueOf(request.safetyInstructionReferences().get(i).getIsFromLibrary()));
            }
        }

        if (request.partReferences() != null) {
            for (int i = 0; i < request.partReferences().size(); i++) {
                var part = request.partReferences().get(i);
                builder.param("partReferences[" + i + "].name", part.getName());
                if (part.getNumber() != null) builder.param("partReferences[" + i + "].number", part.getNumber());
                if (part.getPosition() != null) builder.param("partReferences[" + i + "].position", part.getPosition());
                builder.param("partReferences[" + i + "].materialUnit", part.getMaterialUnit().name());
                builder.param("partReferences[" + i + "].quantity", String.valueOf(part.getQuantity()));
                builder.param("partReferences[" + i + "].supplierCode", part.getSupplierCode());
                if (part.getNote() != null) builder.param("partReferences[" + i + "].note", part.getNote());
            }
        }

        if (request.materialReferences() != null) {
            for (int i = 0; i < request.materialReferences().size(); i++) {
                var material = request.materialReferences().get(i);
                builder.param("materialReferences[" + i + "].name", material.getName());
                builder.param("materialReferences[" + i + "].standard", material.getStandard());
                builder.param("materialReferences[" + i + "].unit", material.getUnit().name());
                builder.param("materialReferences[" + i + "].isFromLibrary", String.valueOf(material.getIsFromLibrary()));
                builder.param("materialReferences[" + i + "].supplierCode", material.getSupplierCode());
                builder.param("materialReferences[" + i + "].position", material.getPosition());
                builder.param("materialReferences[" + i + "].rationingUnit", String.valueOf(material.getRationingUnit()));
                builder.param("materialReferences[" + i + "].consumptionRate", String.valueOf(material.getConsumptionRate()));
                if (material.getNote() != null) builder.param("materialReferences[" + i + "].note", material.getNote());
            }
        }

        if (request.equipmentReference() != null) {
            builder.param("equipmentReference.name", request.equipmentReference().getName());
            builder.param("equipmentReference.index", request.equipmentReference().getIndex());
            builder.param("equipmentReference.isFromLibrary", String.valueOf(request.equipmentReference().getIsFromLibrary()));
            if (request.equipmentReference().getStandard() != null) {
                builder.param("equipmentReference.standard", request.equipmentReference().getStandard());
            }
        }

        if (request.transitions() != null) {
            for (int i = 0; i < request.transitions().size(); i++) {
                builder.param("transitions[" + i + "].number", String.valueOf(request.transitions().get(i).getNumber()));
                builder.param("transitions[" + i + "].content", request.transitions().get(i).getContent());
            }
        }

        if (request.sketchCards() != null && !request.sketchCards().isEmpty()) {
            for (int i = 0; i < request.sketchCards().size(); i++) {
                var sketch = request.sketchCards().get(i);
                if (sketch.getFileName() != null) {
                    builder.param("sketchCards[" + i + "].fileName", sketch.getFileName());
                }
                builder.param("sketchCards[" + i + "].blankType", sketch.getBlankType().name());
                builder.param("sketchCards[" + i + "].sketchSheetNumber", String.valueOf(sketch.getSketchSheetNumber()));
            }
        }

        return builder;
    }
}
