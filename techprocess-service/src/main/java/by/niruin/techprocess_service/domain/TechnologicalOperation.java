package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.AccessType;

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
    private final List<String> workerCodes = new ArrayList<>();
    private final List<SafetyInstructionReference> safetyInstructions = new ArrayList<>();
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
    private final List<PartReference> parts = new ArrayList<>();
    private final List<MaterialReference> materials = new ArrayList<>();
    private final List<TechnologicalTransition> transitions = new ArrayList<>();
    @Setter
    @Getter
    @JsonProperty("sertified")
    private Boolean isSertified;
    @Setter
    @Getter
    private OperationType operationType;
    @Getter
    private final List<SketchCard> sketchCards = new ArrayList<>();

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

    public void addTransition(TechnologicalTransition transition) {
        Objects.requireNonNull(transition);

        this.transitions.add(transition);
    }

    public void addPart(PartReference partReference) {
        Objects.requireNonNull(partReference);

        this.parts.add(partReference);
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
