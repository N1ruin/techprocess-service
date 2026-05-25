package by.niruin.techprocess_service.repository;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.TechnologicalProcessStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnologicalProcessRepository extends MongoRepository<TechnologicalProcess, String> {

    Optional<TechnologicalProcess> findByArchiveNumber(String archiveNumber);

    Optional<TechnologicalProcess> findByPartNumberAndTypeAndWorkshopCode(
            String partNumber, TechnologicalProcessOrganizationType type, String workshopCode);

    Optional<TechnologicalProcess> findFirstByArchiveNumberOrderByRevisionDesc(String archiveNumber);

    Optional<TechnologicalProcess> findByArchiveNumberAndRevision(String archiveNumber, Integer previousRevision);

    boolean existsByArchiveNumberAndStatus(String archiveNumber, TechnologicalProcessStatus status);
}
