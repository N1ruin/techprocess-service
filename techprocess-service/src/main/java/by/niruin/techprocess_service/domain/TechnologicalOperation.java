package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import by.niruin.techprocess_service.domain.enums.OperationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalOperation {
    private Integer number;
    private String name;
    private List<String> workerCodes;
    private final List<SafetyInstructionReference> safetyInstructions = new ArrayList<>();
    private boolean isOnlyForMan;
    private Integer area;
    private Double weight;
    private BlankType blankType;
    private String equipment;
    private final List<PartReference> partReferences = new ArrayList<>();
    private final List<MaterialReference> materialReferences = new ArrayList<>();
    private final List<TechnologicalTransition> transitions = new ArrayList<>();
    private boolean isSertified;
    private OperationType operationType;
    private final List<SketchCard> sketchCards = new ArrayList<>();

    public Integer getNumber() {
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

    public String getEquipment() {
        return equipment;
    }

    public List<PartReference> getProducts() {
        return Collections.unmodifiableList(partReferences);
    }

    public List<MaterialReference> getMaterials() {
        return Collections.unmodifiableList(materialReferences);
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

    public void setNumber(Integer number) {
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

    public void setEquipment(String equipment) {
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

    public void setProducts(List<PartReference> partReferences) {
        Objects.requireNonNull(safetyInstructions);

        this.partReferences.clear();
        this.partReferences.addAll(partReferences);
    }

    public void setMaterials(List<MaterialReference> materialReferences) {
        Objects.requireNonNull(safetyInstructions);

        this.materialReferences.clear();
        this.materialReferences.addAll(materialReferences);
    }

    public void setTransitions(List<TechnologicalTransition> transitions) {
        Objects.requireNonNull(safetyInstructions);

        this.transitions.clear();
        this.transitions.addAll(transitions);
    }

    public void setSketchCards(List<SketchCard> sketchCards) {
        Objects.requireNonNull(safetyInstructions);

        this.sketchCards.clear();
        this.sketchCards.addAll(sketchCards);
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
}
