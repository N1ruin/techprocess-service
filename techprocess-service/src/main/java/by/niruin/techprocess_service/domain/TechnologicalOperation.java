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
    private final List<SafetyInstructionReference> safetyInstructions;

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
    private EquipmentReference equipment;

    private final List<PartReference> parts;
    private final List<MaterialReference> materials;
    private final List<TechnologicalTransition> transitions;
    private final List<ReviewComment> reviewComments;
    private final List<SketchCard> sketchCards;

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
        this.sketchCards = new ArrayList<>();
    }

    @PersistenceCreator
    public TechnologicalOperation(
            List<String> workerCodes,
            List<SafetyInstructionReference> safetyInstructions,
            List<PartReference> parts,
            List<MaterialReference> materials,
            List<TechnologicalTransition> transitions,
            List<ReviewComment> reviewComments,
            List<SketchCard> sketchCards) {

        this.workerCodes = workerCodes != null ? new ArrayList<>(workerCodes) : new ArrayList<>();
        this.safetyInstructions = safetyInstructions != null ? new ArrayList<>(safetyInstructions) : new ArrayList<>();
        this.parts = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
        this.materials = materials != null ? new ArrayList<>(materials) : new ArrayList<>();
        this.transitions = transitions != null ? new ArrayList<>(transitions) : new ArrayList<>();
        this.reviewComments = reviewComments != null ? new ArrayList<>(reviewComments) : new ArrayList<>();
        this.sketchCards = sketchCards != null ? new ArrayList<>(sketchCards) : new ArrayList<>();
    }

    public List<TechnologicalTransition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

    public void setWorkerCodes(List<String> workerCodes) {
        Objects.requireNonNull(workerCodes);
        this.workerCodes.clear();
        this.workerCodes.addAll(workerCodes);
    }

    public void setSafetyInstructions(List<SafetyInstructionReference> safetyInstructions) {
        Objects.requireNonNull(safetyInstructions);
        this.safetyInstructions.clear();
        this.safetyInstructions.addAll(safetyInstructions);
    }

    public void setParts(List<PartReference> partReferences) {
        Objects.requireNonNull(partReferences);
        this.parts.clear();
        this.parts.addAll(partReferences);
    }

    public void setMaterials(List<MaterialReference> materialReferences) {
        Objects.requireNonNull(materialReferences);
        this.materials.clear();
        this.materials.addAll(materialReferences);
    }

    public void setTransitions(List<TechnologicalTransition> transitions) {
        Objects.requireNonNull(transitions);
        this.transitions.clear();
        this.transitions.addAll(transitions);
    }

    public void setSketchCards(List<SketchCard> sketchCards) {
        Objects.requireNonNull(sketchCards);
        this.sketchCards.clear();
        this.sketchCards.addAll(sketchCards);
    }

    public List<ReviewComment> getReviewComments() {
        return Collections.unmodifiableList(reviewComments);
    }

    public List<String> getWorkerCodes() {
        return Collections.unmodifiableList(workerCodes);
    }

    public List<SafetyInstructionReference> getSafetyInstructions() {
        return Collections.unmodifiableList(safetyInstructions);
    }

    public List<PartReference> getParts() {
        return Collections.unmodifiableList(parts);
    }

    public List<MaterialReference> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public List<SketchCard> getSketchCards() {
        return Collections.unmodifiableList(sketchCards);
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
