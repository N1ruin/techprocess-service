package by.niruin.techprocess_service.service;

import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import by.niruin.techprocess_service.model.event.EventType;
import by.niruin.techprocess_service.model.event.MessageBrokerEvent;
import by.niruin.techprocess_service.repository.TransactionOutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
public class TransactionOutboxService {
    private final TransactionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransactionOutboxService(TransactionOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void save(TransactionOutboxRecord outboxRecord) {
        outboxRepository.save(outboxRecord);
    }

    public <T extends MessageBrokerEvent, O> TransactionOutboxRecord createOutboxRecord(EventType eventType, O object,
                                                                                        Function<O, T> eventMapper) {
        var outboxRecord = new TransactionOutboxRecord();
        outboxRecord.setEventType(eventType);

        var event = eventMapper.apply(object);
        outboxRecord.setPayload(objectMapper.writeValueAsString(event));
        outboxRecord.setTimestamp(Instant.now());

        return outboxRecord;
    }

    public Page<TransactionOutboxRecord> findBatchRecords(int batchSize) {
        return outboxRepository.findAll(PageRequest.of(0, batchSize));
    }

    public void deleteAll(List<TransactionOutboxRecord> sentRecords) {
        Objects.requireNonNull(sentRecords);

        if (sentRecords.isEmpty()) {
            return;
        }
        outboxRepository.deleteAll(sentRecords);
    }
}
