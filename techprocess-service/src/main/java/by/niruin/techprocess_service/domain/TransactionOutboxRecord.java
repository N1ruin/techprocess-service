package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.model.event.EventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "outbox_records")
public class TransactionOutboxRecord {
    @Id
    private String id;
    @Setter
    private EventType eventType;
    @Setter
    private String payload;
    @Setter
    @CreatedDate
    private Instant timestamp;

}