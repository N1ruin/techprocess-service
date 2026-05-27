package by.niruin.techprocess_service.repository;

import by.niruin.techprocess_service.domain.TransactionOutboxRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionOutboxRepository extends MongoRepository<TransactionOutboxRecord, Long> {
    List<TransactionOutboxRecord> findAll(PageRequest pageRequest);

}
