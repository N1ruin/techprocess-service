package by.niruin.techprocess_service.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "technological_processes")
@CompoundIndex(name = "part_type_idx",
        def = "{'partNumber': 1, 'type': 1, 'workshopCode': 1, 'revision': 1}",
        unique = true)
public class TechnologicalProcess {
    @Id
    private String id;
    private String name;
    private String partNumber;
    private String archiveNumber;
    private String workshopCode;
    private String workType;
    private TechnologicalProcessOrganizationType type;
    private TechnologicalProcessStatus status;
    private Integer revision;
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant updatedDate;
    private Integer totalSheets;
    private List<TechnologicalOperation> technologicalOperations = new ArrayList<>();
    private List<SketchCard> sketches = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getArchiveNumber() {
        return archiveNumber;
    }

    public void setArchiveNumber(String archiveNumber) {
        this.archiveNumber = archiveNumber;
    }

    public String getWorkshopCode() {
        return workshopCode;
    }

    public void setWorkshopCode(String workshopCode) {
        this.workshopCode = workshopCode;
    }

    public TechnologicalProcessOrganizationType getType() {
        return type;
    }

    public void setType(TechnologicalProcessOrganizationType type) {
        this.type = type;
    }

    public TechnologicalProcessStatus getStatus() {
        return status;
    }

    public void setStatus(TechnologicalProcessStatus status) {
        this.status = status;
    }

    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Instant updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Integer getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(Integer totalSheets) {
        this.totalSheets = totalSheets;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public List<TechnologicalOperation> getTechnologicalOperations() {
        return List.copyOf(technologicalOperations);
    }

    public void setTechnologicalOperations(List<TechnologicalOperation> technologicalOperations) {
        Objects.requireNonNull(technologicalOperations);
        this.technologicalOperations = technologicalOperations;
    }

    public List<SketchCard> getSketches() {
        return List.copyOf(sketches);
    }

    public void setSketches(List<SketchCard> sketches) {
        Objects.requireNonNull(sketches);
        this.sketches = sketches;
    }
}
