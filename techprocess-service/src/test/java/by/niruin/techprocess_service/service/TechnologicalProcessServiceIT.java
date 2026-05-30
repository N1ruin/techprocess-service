package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.EquipmentReference;
import by.niruin.techprocess_service.domain.PartReference;
import by.niruin.techprocess_service.domain.SafetyInstructionReference;
import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.MaterialUnit;
import by.niruin.techprocess_service.domain.enums.OperationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.technological_process.AddOperationRequest;
import by.niruin.techprocess_service.model.technological_process.AddTransitionRequest;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.technological_process.TechnologicalProcessDto;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.security.JwtParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                        })
                )
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
                        })
                )
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
                        })
                )
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
                                })
                        )
                        .content(requestJson))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.CONFLICT.value()));

        assertThat(technologicalProcessRepository.findAll()).hasSize(1);
    }

    @Test
    void cancel_success() throws Exception {
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        )
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        var dto = objectMapper.readValue(response.getResponse().getContentAsString(), TechnologicalProcessDto.class);

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                        dto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                        dto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Иванов");
                                    jwt.claim("last_name", "Иван");
                                    jwt.claim("father_name", "Иванович");
                                })
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                        dto.fullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        )
                )
                .andExpectAll(status().isOk());

        assertThat(technologicalProcessRepository.findByFullNumber(dto.fullNumber()).get().getStatus())
                .isEqualTo(TechnologicalProcessStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowNotFoundException() throws Exception {
        var fullNumber = "1231212312";

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/cancel",
                        fullNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                })
                        )
                )
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }
//Три кейса на статусы IN CORRECTION, CANCELLED и PRODUCTION ошибки

    @Test
    void getByNumberAndRevision_shouldReturnRevisionZero() throws Exception {
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        )
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        var responseBody = response.getResponse().getContentAsString();
        var savedDto = objectMapper.readValue(responseBody, TechnologicalProcessDto.class);

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                        savedDto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })))
                .andExpectAll(
                        jsonPath("$.id").value(savedDto.id()),
                        jsonPath("$.status").value(savedDto.status().name()),
                        jsonPath("$.revision").value(0),
                        jsonPath("$.fullNumber").value(savedDto.fullNumber()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());
    }

    @Test
    void getByNumberAndRevision_shouldThrowNotFound() throws Exception {
        var fullNumber = "41324312";

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                        fullNumber)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void getByNumber_shouldThrowNotFound() throws Exception {
        var fullNumber = "41324312";

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}",
                        fullNumber)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void getByNumberByRevision_shouldThrowNotFound() throws Exception {
        var fullNumber = "41324312";

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}?revision=0",
                        fullNumber)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void sendToReviewAndApproved_shouldReturnStatusApproved() throws Exception {
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);

        var response = mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        )
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        var responseBody = response.getResponse().getContentAsString();
        var savedDto = objectMapper.readValue(responseBody, TechnologicalProcessDto.class);

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                        savedDto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                        savedDto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Иванов");
                                    jwt.claim("last_name", "Иван");
                                    jwt.claim("father_name", "Иванович");
                                })
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/techprocess-service/technological-processes/{full-number}",
                        savedDto.fullNumber())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Иванов");
                                    jwt.claim("last_name", "Иван");
                                    jwt.claim("father_name", "Иванович");
                                })
                        ))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.status").value(TechnologicalProcessStatus.SET_UP.name())
                );
    }

    @Test
    void sendToReview_throwNotFoundException() throws Exception {
        var number = "535432";

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/send-to-review",
                        number)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        ))
                .andExpect(status().isNotFound());
    }

    @Test
    void approve_throwNotFoundException() throws Exception {
        var number = "535432";

        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/approve",
                        number)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Петр");
                                    jwt.claim("last_name", "Петров");
                                    jwt.claim("father_name", "Петрович");
                                })
                        ))
                .andExpect(status().isNotFound());
    }

    @Test
    void addOperation_shouldReturnTechprocess() throws Exception {
        var request = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(request);

        var requestBuilder = post("/api/v1/techprocess-service/technological-processes")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> {
                            jwt.claim("first_name", "Евгений");
                            jwt.claim("last_name", "Лагун");
                            jwt.claim("father_name", "Сергеевич");
                        })
                )
                .content(requestJson);

        var response = mockMvc.perform(requestBuilder)
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
                        jsonPath("$.reviewerApprovedDate").doesNotExist())
                .andReturn();

        var dto = objectMapper.readValue(response.getResponse().getContentAsString(), TechnologicalProcessDto.class);

        var operationRequest = getAddOperationRequest();
        var operationRequestJson = objectMapper.writeValueAsString(operationRequest);

        mockMvc.perform(post("/api/v1//techprocess-service/technological-processes/{full-number}/operations",
                        dto.fullNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                })
                        )
                        .content(operationRequestJson))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(dto.id()),
                        jsonPath("$.status").value(dto.status().name()),
                        jsonPath("$.revision").value(dto.revision()),
                        jsonPath("$.fullNumber").value(dto.fullNumber()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.operations.length()").value(1),
                        jsonPath("$.operations[0].number").value(operationRequest.number()),
                        jsonPath("$.operations[0].name").value(operationRequest.name()),
                        jsonPath("$.reviewerApprovedDate").doesNotExist()
                );
    }

    @Test
    void addOperation_shouldThrowNotFound() throws Exception {
        var operationRequest = getAddOperationRequest();
        var operationRequestJson = objectMapper.writeValueAsString(operationRequest);
        var fullNumber = "12321321.21321.32123";

        mockMvc.perform(post("/api/v1//techprocess-service/technological-processes/{full-number}/operations",
                        fullNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(operationRequestJson)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                })
                        ))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.error").exists(),
                        jsonPath("$.message").exists(),
                        jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void addTransition_success() throws Exception {
        var createTechprocessRequest = createValidRequest();
        var requestJson = objectMapper.writeValueAsString(createTechprocessRequest);
        var createProcessResponse = mockMvc.perform(post("/api/v1/techprocess-service/technological-processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                })
                        ))
                .andExpect(status().isCreated())
                .andReturn();

        var dto = objectMapper.readValue(createProcessResponse.getResponse().getContentAsString(),
                TechnologicalProcessDto.class);

        var operationRequest = getAddOperationRequest();
        var operationRequestJson = objectMapper.writeValueAsString(operationRequest);

        var addOperationResponse = mockMvc.perform(
                        post("/api/v1//techprocess-service/technological-processes/{full-number}/operations",
                                dto.fullNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                        .jwt(jwt -> {
                                            jwt.claim("first_name", "Евгений");
                                            jwt.claim("last_name", "Лагун");
                                            jwt.claim("father_name", "Сергеевич");
                                        })
                                )
                                .content(operationRequestJson))
                .andExpect(status().isCreated())
                .andReturn();

        var addOperationResponseDto = objectMapper.readValue(addOperationResponse.getResponse().getContentAsString(),
                TechnologicalProcessDto.class);

        var addTransitionRequest = getAddTransitionRequest();
        var transitionRequestJson = objectMapper.writeValueAsString(addTransitionRequest);
        var saved = technologicalProcessRepository.findByFullNumber(dto.fullNumber()).get();
        System.out.println("PARTS IN DB: " + saved.getOperations().get(0).getParts());
        mockMvc.perform(post("/api/v1/techprocess-service/technological-processes/{full-number}/operations/{number}/transitions",
                        addOperationResponseDto.fullNumber(), addTransitionRequest.operationNumber())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transitionRequestJson)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> {
                                    jwt.claim("first_name", "Евгений");
                                    jwt.claim("last_name", "Лагун");
                                    jwt.claim("father_name", "Сергеевич");
                                })
                        ))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(addOperationResponseDto.id()),
                        jsonPath("$.status").value(addOperationResponseDto.status().name()),
                        jsonPath("$.revision").value(addOperationResponseDto.revision()),
                        jsonPath("$.fullNumber").value(addOperationResponseDto.fullNumber()),
                        jsonPath("$.createdDate").exists(),
                        jsonPath("$.updatedDate").exists(),
                        jsonPath("$.operations.length()").value(1),
                        jsonPath("$.operations[0].number").value(operationRequest.number()),
                        jsonPath("$.operations[0].name").value(operationRequest.name()),
                        jsonPath("$.operations[0].transitions.length()").value(1),
                        jsonPath("$.operations[0].transitions[0].number").value(addTransitionRequest.number()),
                        jsonPath("$.operations[0].transitions[0].content").value(addTransitionRequest.content()),
                        jsonPath("$.operations[0].transitions[0].equipmentReferences.length()").value(2),
                        jsonPath("$.reviewerApprovedDate").doesNotExist());
    }

    private AddOperationRequest getAddOperationRequest() {
        var equipmentReference = new EquipmentReference();
        equipmentReference.setFromLibrary(false);
        equipmentReference.setIndex("Б517");
        equipmentReference.setName("Стол-верстак");

        var partReference = new PartReference();
        partReference.setName("Вал");
        partReference.setNumber("2022-1312321-Б");
        partReference.setPosition("1");
        partReference.setQuantity(1);
        partReference.setMaterialUnit(MaterialUnit.PIECE);
        partReference.setSupplierCode("342");

        return new AddOperationRequest(
                "005",
                "Сборочная",
                List.of("12345"),
                List.of(new SafetyInstructionReference("101", false)),
                List.of(partReference),
                equipmentReference,
                "4",
                BlankType.OPERATION_BLANK_TITLE,
                1.5,
                1,
                false,
                OperationType.ASSEMBLY);
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

    private AddTransitionRequest getAddTransitionRequest() {
        var equipmentReference1 = new EquipmentReference();
        equipmentReference1.setName("Шуруповерт");
        equipmentReference1.setIndex("Makita DF333DZ");
        equipmentReference1.setFromLibrary(false);

        var equipmentReference2 = new EquipmentReference();
        equipmentReference2.setName("Бита");
        equipmentReference2.setIndex("123123XP");
        equipmentReference2.setFromLibrary(false);

        return new AddTransitionRequest(
                "005",
                "1",
                "Взять кронштейн поз. 1, установить его на плиту поз. 1 и прикрепить винтами поз. 12, затянув их до упора",
                List.of(equipmentReference1, equipmentReference2));
    }
}
