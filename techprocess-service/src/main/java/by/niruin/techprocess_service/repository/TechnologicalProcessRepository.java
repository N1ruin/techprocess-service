package by.niruin.techprocess_service.repository;

import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnologicalProcessRepository extends MongoRepository<TechnologicalProcess, String> {
    boolean existsByOrganizationTypeAndWorkTypeAndArchiveNumber(String organizationType,
                                                                String workType,
                                                                String archiveNumber);

    boolean existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(String organizationType,
                                                                               String workType,
                                                                               String archiveNumber,
                                                                               String workshopCode);

    Optional<TechnologicalProcess> findFirstByArchiveNumberOrderByRevisionDesc(String fullNumber);

    Optional<TechnologicalProcess> findByFullNumberAndRevision(String fullName, int revision);

    Optional<TechnologicalProcess> findFirstByFullNumberAndStatusOrderByRevisionDesc(String fullNumber,
                                                                                     TechnologicalProcessStatus technologicalProcessStatus);

    Optional<TechnologicalProcess> findFirstByFullNumberOrderByRevisionDesc(String fullNumber);

    Page<TechnologicalProcess> findAllByStatus(TechnologicalProcessStatus technologicalProcessStatus, Pageable pageable);
}
