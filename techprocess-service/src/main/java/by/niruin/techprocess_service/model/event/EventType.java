package by.niruin.techprocess_service.model.event;

import lombok.Getter;

@Getter
public enum EventType {
    TECHNOLOGICAL_PROCESS_CREATED("technological-process-topic"),
    TECHNOLOGICAL_PROCESS_UPDATED(TECHNOLOGICAL_PROCESS_CREATED.getTopicName()),
    TECHNOLOGICAL_PROCESS_CANCELLED(TECHNOLOGICAL_PROCESS_CREATED.getTopicName()),
    TECHNOLOGICAL_PROCESS_SENT_TO_REVIEW(TECHNOLOGICAL_PROCESS_CREATED.getTopicName()),
    TECHNOLOGICAL_PROCESS_APPROVED(TECHNOLOGICAL_PROCESS_CREATED.getTopicName()),
    TECHNOLOGICAL_PROCESS_RETURNED_AFTER_REVIEW(TECHNOLOGICAL_PROCESS_CREATED.getTopicName()),
    FILE_MOVE_TO_PERMANENT_STORAGE("file-topic"),
    FILE_DELETED_EVENT(FILE_MOVE_TO_PERMANENT_STORAGE.getTopicName());

    private final String topicName;

    EventType(String topicName) {
        this.topicName = topicName;
    }
}
