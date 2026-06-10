package by.niruin.techprocess_service.unit;

import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import by.niruin.techprocess_service.model.event.EventType;
import by.niruin.techprocess_service.model.event.MessageBrokerEvent;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import by.niruin.techprocess_service.service.TransactionOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionOutboxServiceTest {
    @Mock
    private TransactionOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private TransactionOutboxService transactionOutboxService;

    @Test
    void save_shouldInvokeRepositorySave() {
        var outboxRecord = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("Test")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_CREATED)
                .timestamp(Instant.now())
                .build();

        when(outboxRepository.save(outboxRecord)).thenReturn(outboxRecord);

        transactionOutboxService.save(outboxRecord);

        verify(outboxRepository).save(outboxRecord);
    }

    @Test
    void save_shouldHandleNullPayload() {
        var outboxRecord = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload(null)
                .eventType(EventType.TECHNOLOGICAL_PROCESS_CREATED)
                .timestamp(Instant.now())
                .build();

        when(outboxRepository.save(outboxRecord)).thenReturn(outboxRecord);

        transactionOutboxService.save(outboxRecord);

        verify(outboxRepository).save(outboxRecord);
    }

    @Test
    void createOutboxRecord_shouldReturnOutboxRecord() {
        var testData = "test-data";
        var eventType = EventType.TECHNOLOGICAL_PROCESS_CREATED;
        var expectedPayload = "{\"event\":\"test\"}";
        var mockEvent = mock(MessageBrokerEvent.class);

        when(objectMapper.writeValueAsString(mockEvent)).thenReturn(expectedPayload);

        var outboxRecord = transactionOutboxService.createOutboxRecord(
                eventType,
                testData,
                (data) -> mockEvent);

        assertThat(outboxRecord).isNotNull();
        assertThat(outboxRecord.getEventType()).isEqualTo(eventType);
        assertThat(outboxRecord.getPayload()).isEqualTo(expectedPayload);
        assertThat(outboxRecord.getTimestamp()).isNotNull();
        assertThat(outboxRecord.getTimestamp()).isBeforeOrEqualTo(Instant.now());
        verify(objectMapper).writeValueAsString(mockEvent);
    }

    @Test
    void createOutboxRecord_shouldHandleNullData() {
        var eventType = EventType.TECHNOLOGICAL_PROCESS_CREATED;
        var expectedPayload = "null";
        var mockEvent = mock(MessageBrokerEvent.class);

        when(objectMapper.writeValueAsString(mockEvent)).thenReturn(expectedPayload);

        var outboxRecord = transactionOutboxService.createOutboxRecord(
                eventType,
                null,
                (data) -> mockEvent);

        assertThat(outboxRecord).isNotNull();
        assertThat(outboxRecord.getEventType()).isEqualTo(eventType);
        assertThat(outboxRecord.getPayload()).isEqualTo(expectedPayload);
        assertThat(outboxRecord.getTimestamp()).isNotNull();

        verify(objectMapper).writeValueAsString(mockEvent);
    }

    @Test
    void createOutboxRecord_shouldHandleMapperFunction() {
        var inputData = "raw-data";
        var transformedEvent = mock(MessageBrokerEvent.class);
        var eventType = EventType.TECHNOLOGICAL_PROCESS_UPDATED;
        var expectedPayload = "{\"transformed\":\"data\"}";

        when(objectMapper.writeValueAsString(transformedEvent)).thenReturn(expectedPayload);

        var outboxRecord = transactionOutboxService.createOutboxRecord(
                eventType,
                inputData,
                (data) -> transformedEvent);

        assertThat(outboxRecord).isNotNull();
        assertThat(outboxRecord.getEventType()).isEqualTo(eventType);
        assertThat(outboxRecord.getPayload()).isEqualTo(expectedPayload);
        verify(objectMapper).writeValueAsString(transformedEvent);
    }

    @Test
    void deleteAll_shouldThrowNullPointerException_whenListIsNull() {
        assertThatThrownBy(() -> transactionOutboxService.deleteAll(null))
                .isInstanceOf(NullPointerException.class);

        verify(outboxRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAll_shouldNotInvokeRepository_whenListIsEmpty() {
        var emptyList = new ArrayList<TransactionOutboxRecord>();

        transactionOutboxService.deleteAll(emptyList);

        verify(outboxRepository, never()).deleteAll(any());
    }

    @Test
    void deleteAll_shouldInvokeRepository_whenListHasRecords() {
        var outboxRecord1 = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("testPayload1")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_CREATED)
                .timestamp(Instant.now())
                .build();

        var outboxRecord2 = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("testPayload2")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_UPDATED)
                .timestamp(Instant.now())
                .build();

        var list = List.of(outboxRecord1, outboxRecord2);

        transactionOutboxService.deleteAll(list);

        verify(outboxRepository).deleteAll(list);
    }

    @Test
    void deleteAll_shouldHandleSingleRecord() {
        var outboxRecord = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("testPayload")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_CREATED)
                .timestamp(Instant.now())
                .build();

        var list = List.of(outboxRecord);

        transactionOutboxService.deleteAll(list);

        verify(outboxRepository).deleteAll(list);
    }

    @Test
    void createOutboxRecord_shouldHandleJsonProcessingException() {
        var testData = "invalid-json-data";
        var eventType = EventType.TECHNOLOGICAL_PROCESS_CREATED;
        var mockEvent = mock(MessageBrokerEvent.class);

        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON processing error"));

        assertThatThrownBy(() -> transactionOutboxService.createOutboxRecord(
                eventType,
                testData,
                (data) -> mockEvent))
                .isInstanceOf(RuntimeException.class);

        verify(objectMapper).writeValueAsString(mockEvent);
    }

    @Test
    void save_shouldHandleMultipleSaves() {
        var record1 = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("Test1")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_CREATED)
                .timestamp(Instant.now())
                .build();

        var record2 = TransactionOutboxRecord.builder()
                .id(UUID.randomUUID().toString())
                .payload("Test2")
                .eventType(EventType.TECHNOLOGICAL_PROCESS_UPDATED)
                .timestamp(Instant.now())
                .build();

        when(outboxRepository.save(any(TransactionOutboxRecord.class)))
                .thenReturn(record1)
                .thenReturn(record2);

        transactionOutboxService.save(record1);
        transactionOutboxService.save(record2);

        verify(outboxRepository).save(record1);
        verify(outboxRepository).save(record2);
    }

    @Test
    void createOutboxRecord_shouldSetTimestampCorrectly() {
        var beforeTime = Instant.now();
        var testData = "test-data";
        var eventType = EventType.TECHNOLOGICAL_PROCESS_CREATED;
        var mockEvent = mock(MessageBrokerEvent.class);

        when(objectMapper.writeValueAsString(mockEvent)).thenReturn("payload");

        var outboxRecord = transactionOutboxService.createOutboxRecord(
                eventType,
                testData,
                (data) -> mockEvent);

        var afterTime = Instant.now();

        assertThat(outboxRecord.getTimestamp()).isNotNull();
        assertThat(outboxRecord.getTimestamp()).isAfterOrEqualTo(beforeTime);
        assertThat(outboxRecord.getTimestamp()).isBeforeOrEqualTo(afterTime);
    }
}
