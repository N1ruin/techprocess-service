package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import by.niruin.techprocess_service.model.event.EventType;
import by.niruin.techprocess_service.model.event.file.FileDeletedEvent;
import by.niruin.techprocess_service.model.event.file.MoveFileToPermanentStorageEvent;
import by.niruin.techprocess_service.model.event.technological_process.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxRecordMapper {
    private final ObjectMapper objectMapper;

    public OutboxRecordMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object mapRecordToJson(TransactionOutboxRecord record) {
        var eventType = record.getEventType();
        String payload = record.getPayload();

        return switch (eventType) {
            case FILE_MOVE_TO_PERMANENT_STORAGE ->
                    objectMapper.readValue(payload, MoveFileToPermanentStorageEvent.class);
            case FILE_DELETED_EVENT -> objectMapper.readValue(payload, FileDeletedEvent.class);
            case TECHNOLOGICAL_PROCESS_CREATED ->
                    objectMapper.readValue(payload, TechnologicalProcessCreatedEvent.class);
            case TECHNOLOGICAL_PROCESS_UPDATED ->
                    objectMapper.readValue(payload, TechnologicalProcessUpdatedEvent.class);
            case TECHNOLOGICAL_PROCESS_CANCELLED ->
                    objectMapper.readValue(payload, TechnologicalProcessCancelledEvent.class);
            case TECHNOLOGICAL_PROCESS_APPROVED ->
                    objectMapper.readValue(payload, TechnologicalProcessApprovedEvent.class);
            case TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW ->
                    objectMapper.readValue(payload, TechnologicalProcessSentToReviewEvent.class);
            case TECHNOLOGICAL_PROCESS_RETURNED_AFTER_REVIEW ->
                    objectMapper.readValue(payload, TechnologicalProcessReturnedAfterReviewEvent.class);
        };
    }
}
