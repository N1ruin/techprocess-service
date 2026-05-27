package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessStatus;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Document(collection = "technological_processes")
@CompoundIndex(def = "{'workType': 1, 'archiveNumber': 1, 'organizationType' : 1}", unique = true)
public class TechnologicalProcess {
    @Id
    private String id;
    private String partNumber;
    private String partName;
    private String archiveNumber;
    private String developerLastName;
    private String developerFirstName;
    private String developerFatherName;
    private String reviewerFirstName;
    private String reviewerLastName;
    private String reviewerFatherName;
    private String workshopCode;
    private TechnologicalProcessOrganizationType organizationType;
    private TechnologicalProcessStatus status;
    private TechnologicalProcessWorkType workType;
    private String workName;
    private final List<TechnologicalOperation> operations = new ArrayList<>();
    private final List<ReviewComment> reviewComments = new ArrayList<>();
    private Integer revision;
    @Transient
    private String fullNumber;
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant updatedDate;
    private Instant reviewerApprovedDate;

    public String getId() {
        return id;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public String getPartName() {
        return partName;
    }

    public String getArchiveNumber() {
        return archiveNumber;
    }

    public String getDeveloperLastName() {
        return developerLastName;
    }

    public String getReviewerLastName() {
        return reviewerLastName;
    }

    public String getWorkshopCode() {
        return workshopCode;
    }

    public TechnologicalProcessOrganizationType getOrganizationType() {
        return organizationType;
    }

    public TechnologicalProcessStatus getStatus() {
        return status;
    }

    public TechnologicalProcessWorkType getWorkType() {
        return workType;
    }

    public String getWorkName() {
        return workName;
    }

    public List<TechnologicalOperation> getOperations() {
        return operations;
    }


    public Integer getRevision() {
        return revision;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public void setArchiveNumber(String archiveNumber) {
        this.archiveNumber = archiveNumber;
    }

    public void setWorkshopCode(String workshopCode) {
        this.workshopCode = workshopCode;
    }

    public void setOrganizationType(TechnologicalProcessOrganizationType organizationType) {
        this.organizationType = organizationType;
    }

    public void setStatus(TechnologicalProcessStatus status) {
        this.status = status;
    }

    public void setWorkType(TechnologicalProcessWorkType workType) {
        this.workType = workType;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public void setOperations(List<TechnologicalOperation> operations) {
        Objects.requireNonNull(operations);
        this.operations.clear();
        this.operations.addAll(operations);
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getFullNumber() {
        return fullNumber;
    }

    public void setFullNumber(String fullNumber) {
        this.fullNumber = fullNumber;
    }

    public void setDeveloperLastName(String developerLastName) {
        this.developerLastName = developerLastName;
    }

    public void setReviewerLastName(String reviewerLastName) {
        this.reviewerLastName = reviewerLastName;
    }

    public List<ReviewComment> getReviewComments() {
        return Collections.unmodifiableList(reviewComments);
    }

    public void setReviewComments(List<ReviewComment> reviewComments) {
        Objects.requireNonNull(reviewComments);
        this.reviewComments.clear();
        this.reviewComments.addAll(reviewComments);
    }

    public Instant getReviewerApprovedDate() {
        return reviewerApprovedDate;
    }

    public void setReviewerApprovedDate(Instant reviewerApprovedDate) {
        this.reviewerApprovedDate = reviewerApprovedDate;
    }

    public String getDeveloperFirstName() {
        return developerFirstName;
    }

    public void setDeveloperFirstName(String developerFirstName) {
        this.developerFirstName = developerFirstName;
    }

    public String getDeveloperFatherName() {
        return developerFatherName;
    }

    public void setDeveloperFatherName(String developerFatherName) {
        this.developerFatherName = developerFatherName;
    }

    public String getReviewerFirstName() {
        return reviewerFirstName;
    }

    public void setReviewerFirstName(String reviewerFirstName) {
        this.reviewerFirstName = reviewerFirstName;
    }

    public String getReviewerFatherName() {
        return reviewerFatherName;
    }

    public void setReviewerFatherName(String reviewerFatherName) {
        this.reviewerFatherName = reviewerFatherName;
    }
}
