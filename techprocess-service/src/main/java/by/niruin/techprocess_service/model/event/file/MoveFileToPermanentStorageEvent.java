package by.niruin.techprocess_service.model.event.file;

import by.niruin.techprocess_service.model.event.MessageBrokerEvent;

public record MoveFileToPermanentStorageEvent(String fileName, String eventType) implements MessageBrokerEvent {
}
