package by.niruin.techprocess_service.repository;

import by.niruin.techprocess_service.domain.ReviewComment;
import by.niruin.techprocess_service.domain.TechnologicalOperation;
import by.niruin.techprocess_service.domain.TechnologicalProcess;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TechnologicalProcessRepository extends MongoRepository<TechnologicalProcess, String> {
    boolean existsByOrganizationTypeAndWorkTypeAndArchiveNumber(String organizationType,
                                                                String workType,
                                                                String archiveNumber);

    boolean existsByOrganizationTypeAndWorkTypeAndArchiveNumberAndWorkshopCode(String organizationType,
                                                                               String workType,
                                                                               String archiveNumber,
                                                                               String workshopCode);

    Optional<TechnologicalProcess> findByFullNumberAndRevision(String fullName, int revision);

    Optional<TechnologicalProcess> findFirstByFullNumberAndStatusOrderByRevisionDesc(String fullNumber,
                                                                                     TechnologicalProcessStatus technologicalProcessStatus);

    Optional<TechnologicalProcess> findFirstByFullNumberOrderByRevisionDesc(String fullNumber);

    Page<TechnologicalProcess> findAllByStatus(TechnologicalProcessStatus technologicalProcessStatus, Pageable pageable);

    Optional<TechnologicalProcess> findByFullNumber(String fullNumber);

    Optional<TechnologicalProcess> findByFullNumberAndStatus(String processNumber,
                                                             TechnologicalProcessStatus technologicalProcessStatus);

    @Query("{ '_id': ?0, 'operations.number': ?1 }")
    @Update("{ '$push': { 'operations.$.reviewComments': ?2 } }")
    void addCommentToOperation(String processId, String operationNumber, ReviewComment comment);

    @Query("{ '_id': ?0 }")
    @Update("{ '$push': { 'reviewComments': ?1 } }")
    void addCommentToTechprocess(String processId, ReviewComment comment);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    void updateStatus(String id, TechnologicalProcessStatus status);

    @Query("{ '_id': ?0 }")
    @Update("{ '$push': { 'operations': ?1 } }")
    void addOperation(String id, TechnologicalOperation operation);

    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'operations': { 'number': ?1 } } }")
    void deleteOperation(String id, String operationNumber);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'sentToReviewDate': ?2, 'updatedDate': ?3 } }")
    void sendToReview(String id, TechnologicalProcessStatus status, Instant sentToReviewDate, Instant updatedDate);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'reviewerApprovedDate': ?2 } }")
    void approve(String id, TechnologicalProcessStatus status, Instant reviewerApprovedDate);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'status': ?1 } }")
    void setStatus(String id, TechnologicalProcessStatus status);

    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'reviewComments': { 'uuid': ?1 } } }")
    void removeCommentFromTechprocess(String id, UUID commentUuid);

    @Query("{ '_id': ?0, 'operations.number': ?1 }")
    @Update("{ '$pull': { 'operations.$.reviewComments': { 'uuid': ?2 } } }")
    void removeCommentFromOperation(String id, String operationNumber, UUID commentUuid);


}
