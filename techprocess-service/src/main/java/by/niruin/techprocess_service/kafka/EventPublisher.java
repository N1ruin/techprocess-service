package by.niruin.techprocess_service.kafka;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.mapper.TechnologicalProcessMapper;
import by.niruin.techprocess_service.model.event.EventType;
import by.niruin.techprocess_service.model.event.MessageBrokerEvent;
import by.niruin.techprocess_service.model.event.file.FileDeletedEvent;
import by.niruin.techprocess_service.model.event.file.MoveFileToPermanentStorageEvent;
import by.niruin.techprocess_service.service.TransactionOutboxService;

import java.util.function.Function;

public class EventPublisher {
    private final TransactionOutboxService outboxService;
    private final TechnologicalProcessMapper technologicalProcessMapper;

    public EventPublisher(TransactionOutboxService outboxService, TechnologicalProcessMapper technologicalProcessMapper) {
        this.outboxService = outboxService;
        this.technologicalProcessMapper = technologicalProcessMapper;
    }

    public void publishTechprocessCreatedEvent(TechnologicalProcess technologicalProcess) {
        var techprocessSavedEvent = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_CREATED,
                technologicalProcess,
                technologicalProcessMapper::toCreatedEvent);
        outboxService.save(techprocessSavedEvent);
    }

    public void publishTechprocessCancelledEvent(TechnologicalProcess technologicalProcess) {
        var techprocessCancelledEvent = outboxService.createOutboxRecord(EventType.TECHNOLOGICAL_PROCESS_CANCELLED,
                technologicalProcess,
                technologicalProcessMapper::toCancelledEvent);
        outboxService.save(techprocessCancelledEvent);
    }

    public void publishTechprocessUpdatedEvent(TechnologicalProcess technologicalProcess) {
        var publishTechprocessUpdatedEvent = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_UPDATED,
                technologicalProcess,
                technologicalProcessMapper::toUpdatedEvent);
        outboxService.save(publishTechprocessUpdatedEvent);
    }

    public void publishFileMovedToPermanentStorageEvent(String fileName) {
        var moveFileOutboxRecord = outboxService.createOutboxRecord(
                EventType.FILE_MOVE_TO_PERMANENT_STORAGE,
                fileName,
                (name) ->
                        new MoveFileToPermanentStorageEvent(name, EventType.FILE_MOVE_TO_PERMANENT_STORAGE.name()));
        outboxService.save(moveFileOutboxRecord);
    }

    public void publishFileDeletedEvent(String oldFileName) {
        if (oldFileName != null) {
            var deleteFileOutboxRecord = outboxService.createOutboxRecord(
                    EventType.FILE_DELETED_EVENT,
                    oldFileName,
                    (name) ->
                            new FileDeletedEvent(name, EventType.FILE_DELETED_EVENT.name()));
            outboxService.save(deleteFileOutboxRecord);
        }
    }

    public void publishTechprocessApprovedEvent(TechnologicalProcess technologicalProcess) {
        var processSentToReviewEvent = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_APPROVED,
                technologicalProcess,
                technologicalProcessMapper::toApprovedEvent);
        outboxService.save(processSentToReviewEvent);
    }

    public void publishTechprocessReturnedAfterReviewEvent(TechnologicalProcess technologicalProcess) {
        var processSentToReviewEvent = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW,
                technologicalProcess,
                technologicalProcessMapper::toReturnedAfterReviewEvent);
        outboxService.save(processSentToReviewEvent);
    }

    public void publishProcessSentToReviewEvent(TechnologicalProcess technologicalProcess) {
        var processSentToReviewEvent = outboxService.createOutboxRecord(
                EventType.TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW,
                technologicalProcess,
                technologicalProcessMapper::toSentToReviewEvent);
        outboxService.save(processSentToReviewEvent);
    }

    private void publishTechProcessEvent(EventType eventType,
                                         TechnologicalProcess process,
                                         Function<TechnologicalProcess, ? extends MessageBrokerEvent> mapper) {
        var outboxRecord = outboxService.createOutboxRecord(eventType, process, mapper);
        outboxService.save(outboxRecord);
    }

    private void publishFileEvent(EventType eventType,
                                  String fileName,
                                  Function<String, ? extends MessageBrokerEvent> mapper) {
        if (fileName == null) return;
        var outboxRecord = outboxService.createOutboxRecord(eventType, fileName, mapper);
        outboxService.save(outboxRecord);
    }
}
