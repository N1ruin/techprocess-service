package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessRequest;
import by.niruin.techprocess_service.model.technological_process.CreateTechprocessResponse;
import by.niruin.techprocess_service.repository.TechnologicalProcessRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestConfig.class)
@ActiveProfiles("test")
class TechnologicalProcessServiceIT {
    private static final Logger log = LogManager.getLogger(TechnologicalProcessServiceIT.class);
    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @LocalServerPort
    private int port;

    @Autowired
    TechnologicalProcessRepository repository;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/techprocess-service")
                .build();
        repository.deleteAll();
    }

    @Test
    void shouldSaveSuccessfully() {
        var request = validRequest();

        var response = restClient.post()
                .uri("/technological-processes")
                .body(request)
                .retrieve()
                .toEntity(CreateTechprocessResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotBlank();
        assertThat(body.archiveNumber()).isEqualTo("12345");
        assertThat(body.status()).isEqualTo(TechnologicalProcessStatus.IN_DEVELOPMENT);
        assertThat(body.revision()).isZero();
        assertThat(body.fullNumber()).isNotBlank();
        assertThat(body.developerLastName()).isEqualTo("Лагун");
        assertThat(body.developerFirstName()).isEqualTo("Евгений");
        assertThat(body.createdDate()).isNotNull();
        log.error(repository.findAll().getFirst());
    }

    @Test
    void shouldFailOnDuplicateArchiveNumber() {
        var request = validRequest();
        createProcess(request);

        var ex = assertThrows(HttpClientErrorException.Conflict.class, () ->
                restClient.post()
                        .uri("/technological-processes")
                        .body(request)
                        .retrieve()
                        .toEntity(Void.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldFailOnDuplicateInSameWorkshop() {
        var request = validRequest();
        createProcess(request);

        var ex = assertThrows(HttpClientErrorException.Conflict.class, () ->
                restClient.post()
                        .uri("/technological-processes")
                        .body(request)
                        .retrieve()
                        .toEntity(Void.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldFailOnValidation() {
        var invalidRequest = new CreateTechprocessRequest(
                null, "Вал", "12", "Иванов", "Петров",
                "Геевич", "INVALID", "INVALID", "INVALID", ""
        );

        var ex = assertThrows(HttpClientErrorException.BadRequest.class, () ->
                restClient.post()
                        .uri("/technological-processes")
                        .body(invalidRequest)
                        .retrieve()
                        .toEntity(Void.class)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private CreateTechprocessRequest validRequest() {
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
                "Сборка редуктора"
        );
    }

    private void createProcess(CreateTechprocessRequest request) {
        restClient.post()
                .uri("/technological-processes")
                .body(request)
                .retrieve()
                .toEntity(Void.class);
    }
}