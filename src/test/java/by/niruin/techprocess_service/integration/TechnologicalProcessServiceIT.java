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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
                        jsonPath("$.operations[0].blankType").value(request.blankType()),
                        jsonPath("$.operations[0].equipment.name").value(request.equipment().name()),
                        jsonPath("$.operations[0].parts.length()").value(request.parts().size()),
                        jsonPath("$.operations[0].parts[0].name").value(request.parts().getFirst().name()),
                        jsonPath("$.operations[0].materials.length()").value(request.materials().size()),
                        jsonPath("$.operations[0].materials[0].name")
                                .value(request.materials().getFirst().name()),
                        jsonPath("$.operations[0].safetyInstructions.length()")
                                .value(request.safetyInstructions().size()),
                        jsonPath("$.operations[0].transitions.length()").value(0)
                );

        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations()).hasSize(1);
        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst().getNumber())
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

        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst()).isNotNull();

        var updateRequest = createUpdateOperationRequest();

        mockMvc.perform(updateOperationRequest(saved.getFullNumber(), operation.getNumber(), updateRequest)
                        .with(jwtClaims("Евгений", "Лагун", "Сергеевич", "lagun123")))
                .andExpect(status().isOk());

        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst().getName())
                .isEqualTo(updateRequest.name());
        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst().getNumber())
                .isEqualTo(updateRequest.number());
        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst().getArea())
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
        var existingOperation = savedProcess.getOperations().getFirst();

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

        assertThat(technologicalProcessRepository.findAll().getFirst().getOperations().getFirst().getSketches())
                .hasSize(2);
    }

    @Test
    void updateOperation_keepExistingSketches_success() throws Exception {
        var savedProcess = createProcessWithOperationInDb();
        var existingOperation = savedProcess.getOperations().getFirst();

        var existingFileName1 = "existing-sketch-1.png";
        var existingFileName2 = "existing-sketch-2.png";

        var existingSketch1 = new Sketch();
        existingSketch1.setFileName(existingFileName1);
        existingSketch1.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        existingSketch1.setSketchSheetNumber(1);

        var existingSketch2 = new Sketch();
        existingSketch2.setFileName(existingFileName2);
        existingSketch2.setBlankType(BlankType.OPERATION_BLANK_CONTINUATION);
        existingSketch2.setSketchSheetNumber(2);

        existingOperation.setSketches(List.of(existingSketch1, existingSketch2));
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
        var existingOperation = savedProcess.getOperations().getFirst();

        var existingFileName = "existing-sketch.png";
        Sketch existingSketch = new Sketch();
        existingSketch.setFileName(existingFileName);
        existingSketch.setBlankType(BlankType.OPERATION_BLANK_TITLE);
        existingSketch.setSketchSheetNumber(1);

        existingOperation.setSketches(List.of(existingSketch));
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
        var updatedOperation = updatedProcess.getOperations().getFirst();

        assertThat(updatedOperation.getSketches()).hasSize(3);
        assertThat(updatedOperation.getSketches().getFirst().getFileName()).isEqualTo(existingFileName);
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
        assertThat(updatedOperation.getReviewComments().getFirst().getContent()).isEqualTo("Замечание к операции");
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

        assertThat(updatedProcess.getReviewComments().getFirst().getContent())
                .isEqualTo("Замечание к техпроцессу");

        assertThat(updatedProcess.getReviewComments().getFirst().getUuid())
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

    private RequestPostProcessor jwtClaims(String firstName, String lastName, String fatherName, String username) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> {
                    jwt.claim("username", username);
                    jwt.claim("first_name", firstName);
                    jwt.claim("last_name", lastName);
                    jwt.claim("father_name", fatherName);
                })
                .authorities(new SimpleGrantedAuthority("ROLE_ENGINEER"));
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
        var partDto = PartDto.builder()
                .name("Вал")
                .number("2022-1312321-Б")
                .position("1")
                .quantity(1)
                .materialUnit(MaterialUnit.PIECE.name())
                .supplierCode("342")
                .build();

        var materialDto = MaterialDto.builder()
                .name("Литол-24")
                .standard("ГОСТ 2711-2017")
                .note("Тестовое описание")
                .unit(MaterialUnit.KILOGRAM.name())
                .isFromLibrary(true)
                .supplierCode("384")
                .position("10")
                .rationingUnit(1)
                .consumptionRate(0.005)
                .build();

        var equipmentDto = EquipmentDto.builder()
                .index("Б517")
                .isFromLibrary(false)
                .name("Стол-верстак")
                .build();

        return new AddOperationRequest(
                "005",
                "Сборочная",
                List.of("12345"),
                List.of(new SafetyInstructionDto("101", false)),
                List.of(partDto),
                List.of(materialDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                1.5,
                1,
                false,
                OperationType.ASSEMBLY.name());
    }

    private AddOperationRequest createAddOperationRequest() {
        var partDto = PartDto.builder()
                .name("Вал")
                .position("1*")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE.name())
                .quantity(1)
                .supplierCode("342")
                .build();

        var materialDto = MaterialDto.builder()
                .name("Литол-24")
                .standard("ГОСТ 2711-2017")
                .note("Тестовое описание")
                .unit(MaterialUnit.KILOGRAM.name())
                .isFromLibrary(true)
                .supplierCode("384")
                .position("10")
                .rationingUnit(1)
                .consumptionRate(0.005)
                .build();

        var equipmentDto = EquipmentDto.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        return new AddOperationRequest(
                "005",
                "Сборка",
                List.of("01490"),
                List.of(new SafetyInstructionDto("75", false)),
                List.of(partDto),
                List.of(materialDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                4.5,
                19,
                false,
                OperationType.ASSEMBLY.name());
    }

    private UpdateOperationRequest createUpdateOperationRequest() {
        var partDto = PartDto.builder()
                .name("Кронштейн")
                .position("3")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE.name())
                .quantity(2)
                .supplierCode("385")
                .build();

        var equipmentDto = EquipmentDto.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        var materialDto = MaterialDto.builder()
                .name("Масло И-20А")
                .standard("ГОСТ 2712-2022")
                .note("Тестовое описание")
                .unit(MaterialUnit.LITER.name())
                .isFromLibrary(true)
                .supplierCode("322")
                .position("9")
                .rationingUnit(1)
                .consumptionRate(0.002)
                .build();

        var transitionDto = TechnologicalTransitionDto.builder()
                .number(1)
                .content("Взять кронштейн и смазать маслом")
                .build();

        return new UpdateOperationRequest(
                "001",
                "Слесарная",
                List.of("14092"),
                List.of(new SafetyInstructionDto("100", false)),
                List.of(partDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                10.0,
                1,
                true,
                OperationType.ASSEMBLY.name(),
                List.of(materialDto),
                List.of(transitionDto),
                List.of(),
                List.of());
    }

    private UpdateOperationRequest createUpdateOperationRequestWithSketches(List<SketchDto> sketches,
                                                                            List<MultipartFile> newFiles) {
        var partDto = PartDto.builder()
                .name("Кронштейн")
                .position("3")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE.name())
                .quantity(2)
                .supplierCode("385")
                .build();

        var equipmentDto = EquipmentDto.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        var materialDto = MaterialDto.builder()
                .name("Масло И-20А")
                .standard("ГОСТ 2712-2022")
                .note("Тестовое описание")
                .unit(MaterialUnit.LITER.name())
                .isFromLibrary(true)
                .supplierCode("322")
                .position("9")
                .rationingUnit(1)
                .consumptionRate(0.002)
                .build();

        var transitionDto = TechnologicalTransitionDto.builder()
                .number(1)
                .content("Взять кронштейн и смазать маслом")
                .build();

        return new UpdateOperationRequest(
                "001",
                "Слесарная",
                List.of("14092"),
                List.of(new SafetyInstructionDto("100", false)),
                List.of(partDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                10.0,
                1,
                true,
                OperationType.ASSEMBLY.name(),
                List.of(materialDto),
                List.of(transitionDto),
                sketches,
                newFiles
        );
    }

    private UpdateOperationRequest createUpdateOperationRequestWithNewSketches(List<MultipartFile> newFiles) {
        var sketch1 = SketchDto.builder()
                .fileName(null)
                .blankType(BlankType.OPERATION_BLANK_TITLE.name())
                .sketchSheetNumber(1)
                .operationNumbers(List.of(1, 2))
                .build();

        var sketch2 = SketchDto.builder()
                .fileName(null)
                .blankType(BlankType.OPERATION_BLANK_CONTINUATION.name())
                .sketchSheetNumber(2)
                .operationNumbers(List.of(3, 4))
                .build();

        return createUpdateOperationRequestWithSketches(List.of(sketch1, sketch2), newFiles);
    }

    private UpdateOperationRequest createUpdateOperationRequestWithExistingSketches() {
        var partDto = PartDto.builder()
                .name("Кронштейн")
                .position("3")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE.name())
                .quantity(2)
                .supplierCode("385")
                .build();

        var equipmentDto = EquipmentDto.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        var materialDto = MaterialDto.builder()
                .name("Масло И-20А")
                .standard("ГОСТ 2712-2022")
                .note("Тестовое описание")
                .unit(MaterialUnit.LITER.name())
                .isFromLibrary(true)
                .supplierCode("322")
                .position("9")
                .rationingUnit(1)
                .consumptionRate(0.002)
                .build();

        var transition = TechnologicalTransitionDto.builder()
                .number(1)
                .content("Взять кронштейн и смазать маслом")
                .build();

        var sketch1 = SketchDto.builder()
                .fileName("existing-sketch-1.png")
                .blankType(BlankType.OPERATION_BLANK_TITLE.name())
                .sketchSheetNumber(1)
                .operationNumbers(List.of(1, 2))
                .build();

        var sketch2 = SketchDto.builder()
                .fileName("existing-sketch-2.png")
                .blankType(BlankType.OPERATION_BLANK_CONTINUATION.name())
                .sketchSheetNumber(2)
                .operationNumbers(List.of(3, 4))
                .build();

        return new UpdateOperationRequest(
                "001",
                "Слесарная",
                List.of("14092"),
                List.of(new SafetyInstructionDto("100", false)),
                List.of(partDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                10.0,
                1,
                true,
                OperationType.ASSEMBLY.name(),
                List.of(materialDto),
                List.of(transition),
                List.of(sketch1, sketch2),
                List.of()
        );
    }

    private UpdateOperationRequest createUpdateOperationRequestWithMixedSketches(List<MultipartFile> newFiles) {
        var partDto = PartDto.builder()
                .name("Кронштейн")
                .position("3")
                .note("Тестовое описание")
                .materialUnit(MaterialUnit.PIECE.name())
                .quantity(2)
                .supplierCode("385")
                .build();

        var equipmentDto = EquipmentDto.builder()
                .name("Стол-верстак")
                .index("1234-1234")
                .isFromLibrary(false)
                .build();

        var materialDto = MaterialDto.builder()
                .name("Масло И-20А")
                .standard("ГОСТ 2712-2022")
                .note("Тестовое описание")
                .unit(MaterialUnit.LITER.name())
                .isFromLibrary(true)
                .supplierCode("322")
                .position("9")
                .rationingUnit(1)
                .consumptionRate(0.002)
                .build();

        var transition = TechnologicalTransitionDto.builder()
                .number(1)
                .content("Взять кронштейн и смазать маслом")
                .build();

        var existingSketch = SketchDto.builder()
                .fileName("existing-sketch.png")
                .blankType(BlankType.OPERATION_BLANK_TITLE.name())
                .sketchSheetNumber(1)
                .operationNumbers(List.of(1))
                .build();

        var newSketch1 = SketchDto.builder()
                .fileName(null)
                .blankType(BlankType.OPERATION_BLANK_CONTINUATION.name())
                .sketchSheetNumber(2)
                .operationNumbers(List.of(2, 3))
                .build();

        var newSketch2 = SketchDto.builder()
                .fileName(null)
                .blankType(BlankType.OPERATION_BLANK_TITLE.name())
                .sketchSheetNumber(3)
                .operationNumbers(List.of(4))
                .build();

        return new UpdateOperationRequest(
                "001",
                "Слесарная",
                List.of("14092"),
                List.of(new SafetyInstructionDto("100", false)),
                List.of(partDto),
                equipmentDto,
                "4",
                BlankType.OPERATION_BLANK_TITLE.name(),
                10.0,
                1,
                true,
                OperationType.ASSEMBLY.name(),
                List.of(materialDto),
                List.of(transition),
                List.of(existingSketch, newSketch1, newSketch2),
                newFiles
        );
    }

    private MockMultipartHttpServletRequestBuilder updateOperationRequest(String fullNumber, String operationNumber,
                                                                          UpdateOperationRequest request) {
        var builder = multipart(HttpMethod.PUT,
                "/api/v1/techprocess-service/technological-processes/{full-number}/operations/{operation-number}",
                fullNumber, operationNumber);

        builder.param("number", request.number());
        builder.param("name", request.name());
        builder.param("blankType", request.blankType());
        builder.param("operationType", request.operationType());
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

        if (request.safetyInstructions() != null) {
            for (int i = 0; i < request.safetyInstructions().size(); i++) {
                builder.param("safetyInstructions[" + i + "].number",
                        request.safetyInstructions().get(i).number());
                builder.param("safetyInstructions[" + i + "].fromLibrary",
                        String.valueOf(request.safetyInstructions().get(i).isFromLibrary()));
            }
        }

        if (request.parts() != null) {
            for (int i = 0; i < request.parts().size(); i++) {
                var part = request.parts().get(i);
                builder.param("parts[" + i + "].name", part.name());
                if (part.number() != null) builder.param("parts[" + i + "].number", part.number());
                if (part.position() != null) builder.param("parts[" + i + "].position", part.position());
                builder.param("parts[" + i + "].materialUnit", part.materialUnit());
                builder.param("parts[" + i + "].quantity", String.valueOf(part.quantity()));
                builder.param("parts[" + i + "].supplierCode", part.supplierCode());
                if (part.note() != null) builder.param("parts[" + i + "].note", part.note());
            }
        }

        if (request.materials() != null) {
            for (int i = 0; i < request.materials().size(); i++) {
                var material = request.materials().get(i);
                builder.param("materials[" + i + "].name", material.name());
                builder.param("materials[" + i + "].standard", material.standard());
                builder.param("materials[" + i + "].unit", material.unit());
                builder.param("materials[" + i + "].isFromLibrary", String.valueOf(material.isFromLibrary()));
                builder.param("materials[" + i + "].supplierCode", material.supplierCode());
                builder.param("materials[" + i + "].position", material.position());
                builder.param("materials[" + i + "].rationingUnit", String.valueOf(material.rationingUnit()));
                builder.param("materials[" + i + "].consumptionRate", String.valueOf(material.consumptionRate()));
                if (material.note() != null) builder.param("materials[" + i + "].note", material.note());
            }
        }

        if (request.equipment() != null) {
            builder.param("equipments.name", request.equipment().name());
            builder.param("equipments.index", request.equipment().index());
            builder.param("equipments.isFromLibrary", String.valueOf(request.equipment().isFromLibrary()));
            if (request.equipment().standard() != null) {
                builder.param("equipments.standard", request.equipment().standard());
            }
        }

        if (request.transitions() != null) {
            for (int i = 0; i < request.transitions().size(); i++) {
                builder.param("transitions[" + i + "].number", String.valueOf(request.transitions().get(i).number()));
                builder.param("transitions[" + i + "].content", request.transitions().get(i).content());
            }
        }

        if (request.sketches() != null && !request.sketches().isEmpty()) {
            for (int i = 0; i < request.sketches().size(); i++) {
                var sketch = request.sketches().get(i);
                if (sketch.fileName() != null) {
                    builder.param("sketches[" + i + "].fileName", sketch.fileName());
                }
                builder.param("sketches[" + i + "].blankType", sketch.blankType());
                builder.param("sketches[" + i + "].sketchSheetNumber", String.valueOf(sketch.sketchSheetNumber()));
            }
        }

        return builder;
    }
}
