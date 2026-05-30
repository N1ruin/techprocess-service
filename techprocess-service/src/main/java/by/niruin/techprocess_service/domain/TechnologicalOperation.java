package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.AccessType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalOperation {
    private String number;
    private String name;
    private final List<String> workerCodes = new ArrayList<>();
    private final List<SafetyInstructionReference> safetyInstructions = new ArrayList<>();
    @JsonProperty("onlyForMan")
    private boolean isOnlyForMan;
    private Integer area;
    private Double weight;
    private BlankType blankType;
    private EquipmentReference equipment;
    @AccessType(AccessType.Type.FIELD)
    private final List<PartReference> parts = new ArrayList<>();
    private final List<MaterialReference> materials = new ArrayList<>();
    private final List<TechnologicalTransition> transitions = new ArrayList<>();
    @JsonProperty("sertified")
    private boolean isSertified;
    private OperationType operationType;
    private final List<SketchCard> sketchCards = new ArrayList<>();

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public List<String> getWorkerCodes() {
        return Collections.unmodifiableList(workerCodes);
    }

    public List<SafetyInstructionReference> getSafetyInstructions() {
        return Collections.unmodifiableList(safetyInstructions);
    }

    public boolean isOnlyForMan() {
        return isOnlyForMan;
    }

    public Integer getArea() {
        return area;
    }

    public Double getWeight() {
        return weight;
    }

    public BlankType getBlankType() {
        return blankType;
    }

    public EquipmentReference getEquipment() {
        return equipment;
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

    public boolean isSertified() {
        return isSertified;
    }

    public List<SketchCard> getSketchCards() {
        return sketchCards;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkerCodes(List<String> workerCodes) {
        Objects.requireNonNull(workerCodes);

        this.workerCodes.clear();
        this.workerCodes.addAll(workerCodes);
    }

    public void setOnlyForMan(boolean onlyForMan) {
        isOnlyForMan = onlyForMan;
    }

    public void setArea(Integer area) {
        this.area = area;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public void setBlankType(BlankType blankType) {
        this.blankType = blankType;
    }

    public void setEquipment(EquipmentReference equipment) {
        this.equipment = equipment;
    }

    public void setSertified(boolean sertified) {
        isSertified = sertified;
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

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public void addTransition(TechnologicalTransition transition) {
        Objects.requireNonNull(transition);

        this.transitions.add(transition);
    }

    public void addPart(PartReference partReference) {
        Objects.requireNonNull(partReference);

        this.parts.add(partReference);
    }
}
