package by.niruin.techprocess_service.repository;

import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionOutboxRepository extends MongoRepository<TransactionOutboxRecord, String> {
}
