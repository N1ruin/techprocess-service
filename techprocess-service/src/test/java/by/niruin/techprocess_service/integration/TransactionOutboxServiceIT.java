package by.niruin.techprocess_service.integration;

import by.niruin.techprocess_service.config.SchedulerConfig;
import by.niruin.techprocess_service.config.SchedulingOutboxProperties;
import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import by.niruin.techprocess_service.model.event.EventType;
import by.niruin.techprocess_service.model.event.file.FileDeletedEvent;
import by.niruin.techprocess_service.model.event.file.MoveFileToPermanentStorageEvent;
import by.niruin.techprocess_service.model.event.technological_process.*;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.service.TransactionOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Import(SchedulerConfig.class)
class TransactionOutboxServiceIT extends BaseIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7.0")
            .withReplicaSet();

    @Autowired
    private TransactionOutboxService outboxService;

    @Autowired
    private TransactionOutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchedulingOutboxProperties schedulingOutboxProperties;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
    }

    @Test
    void save_shouldSaveOutboxRecord() {
        var outboxRecord = createTestOutboxRecord();

        outboxService.save(outboxRecord);

        var savedRecord = outboxRepository.findById(outboxRecord.getId()).orElse(null);
        assertNotNull(savedRecord);
        assertEquals(outboxRecord.getEventType(), savedRecord.getEventType());
        assertEquals(outboxRecord.getPayload(), savedRecord.getPayload());
        assertNotNull(savedRecord.getTimestamp());
    }

    @Test
    void createOutboxRecord_shouldCreateValidRecordForTechnologicalProcessCreatedEvent() {
        var eventType = EventType.TECHNOLOGICAL_PROCESS_CREATED;
        var process = createTestTechnologicalProcess();

        var outboxRecord = outboxService.createOutboxRecord(
                eventType,
                process,
                p -> new TechnologicalProcessCreatedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        assertNotNull(outboxRecord);
        assertEquals(eventType, outboxRecord.getEventType());
        assertNotNull(outboxRecord.getPayload());
        assertNotNull(outboxRecord.getTimestamp());
    }

    @Test
    void createOutboxRecord_shouldSerializeTechnologicalProcessCreatedEventCorrectly() {
        var process = createTestTechnologicalProcess();
        var expectedEvent = new TechnologicalProcessCreatedEvent(process.getFullNumber(), process.getPartName(), process.getPartNumber());
        var expectedPayload = objectMapper.writeValueAsString(expectedEvent);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_CREATED,
                process,
                p -> new TechnologicalProcessCreatedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        assertEquals(expectedPayload, outboxRecord.getPayload());
    }

    @Test
    void findBatchRecords_shouldReturnEmptyList_whenNoRecords() {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertTrue(batchRecords.isEmpty());
    }

    @Test
    void findBatchRecords_shouldReturnRecords() {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_UPDATED);
        createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CANCELLED);

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(3, batchRecords.getContent().size());
    }

    @Test
    void findBatchRecords_shouldReturnLessThanBatchSize_whenNotEnoughRecords() {
        var batchSize = schedulingOutboxProperties.getBatchSize();

        for (int i = 0; i < batchSize - 5; i++) {
            createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        }

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(batchSize - 5, batchRecords.getContent().size());
    }

    @Test
    void deleteAll_shouldRemoveAllGivenRecords() {
        var record1 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        var record2 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_UPDATED);
        var record3 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CANCELLED);
        var recordsToDelete = List.of(record1, record2);

        outboxService.deleteAll(recordsToDelete);

        assertTrue(outboxRepository.findById(record1.getId()).isEmpty());
        assertTrue(outboxRepository.findById(record2.getId()).isEmpty());
        assertTrue(outboxRepository.findById(record3.getId()).isPresent());
    }

    @Test
    void deleteAll_shouldDoNothing_whenEmptyList() {
        var record = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        List<TransactionOutboxRecord> emptyList = List.of();

        outboxService.deleteAll(emptyList);

        assertThat(outboxRepository.findById(record.getId())).isPresent();
    }

    @Test
    void deleteAll_shouldThrowNullPointerException_whenNullList() {
        assertThrows(NullPointerException.class, () -> outboxService.deleteAll(null));
    }

    @Test
    void findBatchRecords_shouldReturnRecordsWithCorrectTimestamp() throws InterruptedException {
        var batchSize = schedulingOutboxProperties.getBatchSize();
        var beforeSave = Instant.now();
        Thread.sleep(2);
        createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        Thread.sleep(2);
        var afterSave = Instant.now();

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(1, batchRecords.getContent().size());
        var record = batchRecords.getContent().get(0);
        assertNotNull(record.getTimestamp());
        assertTrue(record.getTimestamp().isAfter(beforeSave) || record.getTimestamp().equals(beforeSave));
        assertTrue(record.getTimestamp().isBefore(afterSave) || record.getTimestamp().equals(afterSave));
    }

    @Test
    void createOutboxRecord_shouldSetTimestampToCurrentTime() throws InterruptedException {
        var beforeCreate = Instant.now();
        Thread.sleep(1);

        var outboxRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_CREATED,
                createTestTechnologicalProcess(),
                p -> new TechnologicalProcessCreatedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        Thread.sleep(1);
        var afterCreate = Instant.now();

        assertNotNull(outboxRecord.getTimestamp());
        assertTrue(outboxRecord.getTimestamp().isAfter(beforeCreate));
        assertTrue(outboxRecord.getTimestamp().isBefore(afterCreate));
    }

    @Test
    void save_shouldPersistMultipleRecords() {
        var record1 = createTestOutboxRecord();
        var record2 = createTestOutboxRecord();
        var record3 = createTestOutboxRecord();

        outboxService.save(record1);
        outboxService.save(record2);
        outboxService.save(record3);

        var allRecords = outboxRepository.findAll();
        assertEquals(3, allRecords.size());
    }

    @Test
    void createOutboxRecord_shouldHandleDifferentTechnologicalProcessEventTypes() {
        var process = createTestTechnologicalProcess();

        var createdRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_CREATED,
                process,
                p -> new TechnologicalProcessCreatedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        var updatedRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_UPDATED,
                process,
                p -> new TechnologicalProcessUpdatedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        var cancelledRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_CANCELLED,
                process,
                p -> new TechnologicalProcessCancelledEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        var sentToReviewRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW,
                process,
                p -> new TechnologicalProcessSentToReviewEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        var approvedRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_APPROVED,
                process,
                p -> new TechnologicalProcessApprovedEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        var returnedRecord = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_RETURNED_AFTER_REVIEW,
                process,
                p -> new TechnologicalProcessReturnedAfterReviewEvent(p.getFullNumber(), p.getPartName(), p.getPartNumber())
        );

        assertEquals(EventType.TECHNOLOGICAL_PROCESS_CREATED, createdRecord.getEventType());
        assertEquals(EventType.TECHNOLOGICAL_PROCESS_UPDATED, updatedRecord.getEventType());
        assertEquals(EventType.TECHNOLOGICAL_PROCESS_CANCELLED, cancelledRecord.getEventType());
        assertEquals(EventType.TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW, sentToReviewRecord.getEventType());
        assertEquals(EventType.TECHNOLOGICAL_PROCESS_APPROVED, approvedRecord.getEventType());
        assertEquals(EventType.TECHNOLOGICAL_PROCESS_RETURNED_AFTER_REVIEW, returnedRecord.getEventType());
    }

    @Test
    void createOutboxRecord_shouldHandleFileEventTypes() {
        var fileName = "test-file.txt";

        var moveFileRecord = outboxService.createOutboxRecord(
                EventType.FILE_MOVE_TO_PERMANENT_STORAGE,
                fileName,
                f -> new MoveFileToPermanentStorageEvent(f, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name())
        );

        var deleteFileRecord = outboxService.createOutboxRecord(
                EventType.FILE_DELETED_EVENT,
                fileName,
                f -> new FileDeletedEvent(f, EventType.FILE_DELETED_EVENT.name())
        );

        assertEquals(EventType.FILE_MOVE_TO_PERMANENT_STORAGE, moveFileRecord.getEventType());
        assertEquals(EventType.FILE_DELETED_EVENT, deleteFileRecord.getEventType());
    }

    @Test
    void findBatchRecords_shouldRespectBatchSize() {
        var batchSize = schedulingOutboxProperties.getBatchSize();

        for (int i = 0; i < batchSize * 2; i++) {
            createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        }

        var batchRecords = outboxService.findBatchRecords(batchSize);

        assertNotNull(batchRecords);
        assertEquals(batchSize, batchRecords.getContent().size());
        assertEquals(0, batchRecords.getNumber());
        assertEquals(batchSize, batchRecords.getSize());
    }

    @Test
    void deleteAll_shouldDeleteOnlySpecifiedRecords() {
        var record1 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        var record2 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_UPDATED);
        var record3 = createAndSaveOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CANCELLED);

        outboxService.deleteAll(List.of(record1, record3));

        assertTrue(outboxRepository.findById(record1.getId()).isEmpty());
        assertTrue(outboxRepository.findById(record2.getId()).isPresent());
        assertTrue(outboxRepository.findById(record3.getId()).isEmpty());
    }

    private TransactionOutboxRecord createTestOutboxRecord() {
        var record = new TransactionOutboxRecord();
        record.setEventType(EventType.TECHNOLOGICAL_PROCESS_CREATED);
        record.setPayload("{\"test\":\"payload\"}");
        record.setTimestamp(Instant.now());
        return record;
    }

    private TransactionOutboxRecord createAndSaveOutboxRecord(EventType eventType) {
        var record = new TransactionOutboxRecord();
        record.setEventType(eventType);
        record.setPayload("{\"test\":\"payload\"}");
        record.setTimestamp(Instant.now());
        outboxService.save(record);
        return record;
    }

    private TechnologicalProcess createTestTechnologicalProcess() {
        var process = new TechnologicalProcess();
        process.setFullNumber(UUID.randomUUID().toString());
        process.setPartName("Тестовая деталь");
        process.setPartNumber("ДЕТ-1234567");
        process.setArchiveNumber("12345");
        process.setWorkshopCode("001");
        return process;
    }
}