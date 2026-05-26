package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalOperation {
    private Integer number;
    private String name;
    private String workerCode;
    private final List<SafetyInstructionReference> safetyInstructions = new ArrayList<>();
    private boolean isOnlyForWoman;
    private Integer sectionNumber;
    private String weight;
    private BlankType blankType;
    private String equipment;
    private final List<Part> parts = new ArrayList<>();
    private final List<MaterialReference> materialReferences = new ArrayList<>();
    private final List<TechnologicalTransition> transitions = new ArrayList<>();
    private boolean isSertified;
    private final List<SketchCard> sketchCards = new ArrayList<>();

    public Integer getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getWorkerCode() {
        return workerCode;
    }

    public List<SafetyInstructionReference> getSafetyInstructions() {
        return Collections.unmodifiableList(safetyInstructions);
    }

    public boolean isOnlyForWoman() {
        return isOnlyForWoman;
    }

    public Integer getSectionNumber() {
        return sectionNumber;
    }

    public String getWeight() {
        return weight;
    }

    public BlankType getBlankType() {
        return blankType;
    }

    public String getEquipment() {
        return equipment;
    }

    public List<Part> getProducts() {
        return Collections.unmodifiableList(parts);
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

    public void setWorkerCode(String workerCode) {
        this.workerCode = workerCode;
    }

    public void setOnlyForWoman(boolean onlyForWoman) {
        isOnlyForWoman = onlyForWoman;
    }

    public void setSectionNumber(Integer sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public void setWeight(String weight) {
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

    public void setProducts(List<Part> parts) {
        Objects.requireNonNull(safetyInstructions);

        this.parts.clear();
        this.parts.addAll(parts);
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
}
