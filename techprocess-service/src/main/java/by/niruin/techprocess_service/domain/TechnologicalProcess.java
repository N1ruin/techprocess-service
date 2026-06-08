package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.*;

@Document(collection = "technological_processes")
@CompoundIndex(def = "{'workType': 1, 'archiveNumber': 1, 'organizationType' : 1, 'revision': 1}", unique = true)
public class TechnologicalProcess {
    @Setter
    @Getter
    @Id
    private String id;
    @Setter
    @Getter
    private String partNumber;
    @Setter
    @Getter
    private String partName;
    @Setter
    @Getter
    private String routeCardNote;
    @Setter
    @Getter
    private String archiveNumber;
    @Setter
    @Getter
    private String developerUsername;
    @Setter
    @Getter
    private String developerLastName;
    @Setter
    @Getter
    private String developerFirstName;
    @Setter
    @Getter
    private String developerFatherName;
    @Setter
    @Getter
    private String reviewerUsername;
    @Setter
    @Getter
    private String reviewerFirstName;
    @Setter
    @Getter
    private String reviewerLastName;
    @Setter
    @Getter
    private String reviewerFatherName;
    @Setter
    @Getter
    private String workshopCode;
    @Setter
    @Getter
    private TechnologicalProcessOrganizationType organizationType;
    @Setter
    @Getter
    private TechnologicalProcessStatus status;
    @Setter
    @Getter
    private TechnologicalProcessWorkType workType;
    @Setter
    @Getter
    private String workName;
    private final List<TechnologicalOperation> operations;
    private final List<ReviewComment> reviewComments;
    @Setter
    @Getter
    private Integer revision;
    @Setter
    @Getter
    private String fullNumber;
    @Setter
    @Getter
    @CreatedDate
    private Instant createdDate;
    @Setter
    @Getter
    @LastModifiedDate
    private Instant updatedDate;
    @Setter
    @Getter
    private Instant sentToReviewDate;
    @Setter
    @Getter
    private Instant reviewerApprovedDate;

    public TechnologicalProcess() {
        this.operations = new ArrayList<>();
        this.reviewComments = new ArrayList<>();
    }

    @PersistenceCreator
    public TechnologicalProcess(List<TechnologicalOperation> operations, List<ReviewComment> reviewComments) {
        this.operations = operations != null ? new ArrayList<>(operations) : new ArrayList<>();
        this.reviewComments = reviewComments != null ? new ArrayList<>(reviewComments) : new ArrayList<>();
    }

    public List<TechnologicalOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public List<ReviewComment> getReviewComments() {
        return Collections.unmodifiableList(reviewComments);
    }

    public void setOperations(List<TechnologicalOperation> operations) {
        Objects.requireNonNull(operations);
        this.operations.clear();
        this.operations.addAll(operations);
    }

    public void addOperation(TechnologicalOperation operation) {
        Objects.requireNonNull(operation);
        operations.add(operation);
    }

    public void addReviewComment(ReviewComment comment) {
        Objects.requireNonNull(comment);
        this.reviewComments.add(comment);
    }
}