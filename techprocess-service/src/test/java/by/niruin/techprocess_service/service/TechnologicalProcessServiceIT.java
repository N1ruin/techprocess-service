package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.security.JwtParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class TechnologicalProcessServiceIT {
    private static final Logger log = LogManager.getLogger(TechnologicalProcessServiceIT.class);
    @Container
    @ServiceConnection
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0");
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

    @BeforeEach
    void cleanDatabase() {
        technologicalProcessRepository.deleteAll();
        transactionOutboxRepository.deleteAll();
    }

    @Test
    void save_success() throws Exception {
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);
        var requestBuilder = post("/api/v1/techprocess-service/technological-processes")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> {
                            jwt.claim("first_name", "Евгений");
                            jwt.claim("last_name", "Лагун");
                            jwt.claim("father_name", "Сергеевич");
                        }))
                .content(requestJson);

        mockMvc.perform(requestBuilder)
                .andExpectAll(
                        status().isCreated(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json(requestJson),
                        jsonPath("$.id").exists(),
                        jsonPath("$.status").value(TechnologicalProcessStatus.IN_DEVELOPMENT.name()),
                        jsonPath("$.revision").value(0),
                        jsonPath("$.fullNumber").value("100316761292.02188.12345"),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());
        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
        assertThat(transactionOutboxRepository.findAll()).hasSize(1);
    }

    @Test
    void save_shouldThrowsValidationException() throws Exception {
        var request = createInvalidRequest();
        var requestJson = objectMapper.writeValueAsString(request);
        var requestBuilder = post("/api/v1/techprocess-service/technological-processes")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> {
                            jwt.claim("first_name", "Евгений");
                            jwt.claim("last_name", "Лагун");
                            jwt.claim("father_name", "Сергеевич");
                        }))
                .content(requestJson);

        mockMvc.perform(requestBuilder)
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
        var requestJson = objectMapper.writeValueAsString(request);
        var requestBuilder = post("/api/v1/techprocess-service/technological-processes")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> {
                            jwt.claim("first_name", "Евгений");
                            jwt.claim("last_name", "Лагун");
                            jwt.claim("father_name", "Сергеевич");
                        }))
                .content(requestJson);

        mockMvc.perform(requestBuilder)
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
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                }))
                        .content(requestJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                }))
                        .content(requestJson))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.CONFLICT.value()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
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
                "Сборка редуктора");
    }

    private CreateTechprocessRequest createRequestWithSelfReviewer() {
        return new CreateTechprocessRequest(
                "ДЕТ-1234567",
                "Вал-шестерня",
                "12345",
                jwtParser.getFirstName(),
                jwtParser.getLastName(),
                jwtParser.getFatherName(),
                "003",
                "ASSEMBLY",
                "SINGLE",
                "Сборка редуктора");
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
                "1239fdsa");
    }
}
