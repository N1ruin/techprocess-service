package by.niruin.techprocess_service.model.event.technological_process;

import by.niruin.techprocess_service.model.event.MessageBrokerEvent;

public record TechnologicalProcessReturnedAfterReviewEvent(String fullNumber,
                                                           String partName,
                                                           String partNumber) implements MessageBrokerEvent {
}
