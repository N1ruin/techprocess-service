package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalOperation {
    @Getter
    @Setter
    private String number;
    @Getter
    @Setter
    private String name;

    private final List<String> workerCodes;
    private final List<SafetyInstruction> safetyInstructions;

    @Setter
    @Getter
    @JsonProperty("onlyForMan")
    private Boolean isOnlyForMan;
    @Setter
    @Getter
    private Integer area;
    @Setter
    @Getter
    private Double weight;
    @Setter
    @Getter
    private BlankType blankType;
    @Setter
    @Getter
    private Equipment equipment;

    private final List<Part> parts;
    private final List<Material> materials;
    private final List<TechnologicalTransition> transitions;
    private final List<ReviewComment> reviewComments;
    private final List<Sketch> sketches;

    @Setter
    @Getter
    @JsonProperty("sertified")
    private Boolean isSertified;
    @Setter
    @Getter
    private OperationType operationType;

    public TechnologicalOperation() {
        this.workerCodes = new ArrayList<>();
        this.safetyInstructions = new ArrayList<>();
        this.parts = new ArrayList<>();
        this.materials = new ArrayList<>();
        this.transitions = new ArrayList<>();
        this.reviewComments = new ArrayList<>();
        this.sketches = new ArrayList<>();
    }

    @PersistenceCreator
    public TechnologicalOperation(
            List<String> workerCodes,
            List<SafetyInstruction> safetyInstructions,
            List<Part> parts,
            List<Material> materials,
            List<TechnologicalTransition> transitions,
            List<ReviewComment> reviewComments,
            List<Sketch> sketches) {

        this.workerCodes = workerCodes != null ? new ArrayList<>(workerCodes) : new ArrayList<>();
        this.safetyInstructions = safetyInstructions != null ? new ArrayList<>(safetyInstructions) : new ArrayList<>();
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
        this.materials = materials != null ? new ArrayList<>(materials) : new ArrayList<>();
        this.transitions = transitions != null ? new ArrayList<>(transitions) : new ArrayList<>();
        this.reviewComments = reviewComments != null ? new ArrayList<>(reviewComments) : new ArrayList<>();
        this.sketches = sketches != null ? new ArrayList<>(sketches) : new ArrayList<>();
    }

    public List<TechnologicalTransition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    public void setWorkerCodes(List<String> workerCodes) {
        this.workerCodes.clear();

        if (workerCodes != null) {
            this.workerCodes.addAll(workerCodes);
        }
    }

    public void setSafetyInstructions(List<SafetyInstruction> safetyInstructions) {
        this.safetyInstructions.clear();

        if (safetyInstructions != null) {
            this.safetyInstructions.addAll(safetyInstructions);
        }
    }

    public void setParts(List<Part> parts) {
        this.parts.clear();

        if (parts != null) {
            this.parts.addAll(parts);
        }
    }

    public void setMaterials(List<Material> materials) {
        this.materials.clear();

        if (materials != null) {
            this.materials.addAll(materials);
        }
    }

    public void setTransitions(List<TechnologicalTransition> transitions) {
        this.transitions.clear();

        if (transitions != null) {
            this.transitions.addAll(transitions);
        }
    }

    public void setSketches(List<Sketch> sketches) {
        this.sketches.clear();

        if (sketches != null) {
            this.sketches.addAll(sketches);
        }
    }

    public void setReviewComments(List<ReviewComment> comments) {
        this.reviewComments.clear();

        if (comments != null) {
            this.reviewComments.addAll(comments);
        }
    }

    public List<ReviewComment> getReviewComments() {
        return Collections.unmodifiableList(reviewComments);
    }

    public List<String> getWorkerCodes() {
        return Collections.unmodifiableList(workerCodes);
    }

    public List<SafetyInstruction> getSafetyInstructions() {
        return Collections.unmodifiableList(safetyInstructions);
    }

    public List<Part> getParts() {
        return Collections.unmodifiableList(parts);
    }

    public List<Material> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public List<Sketch> getSketches() {
        return Collections.unmodifiableList(sketches);
    }

    public void addReviewComment(ReviewComment comment) {
        Objects.requireNonNull(comment);
        this.reviewComments.add(comment);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TechnologicalOperation that = (TechnologicalOperation) o;
        return Objects.equals(number, that.number) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, name);
    }
}
