package by.niruin.techprocess_service.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TechnologicalOperation {
    private String number;
    private String name;
    private String workplace;
    private String equipment;
    private String workerCode;
    private String workerCategory;
    private List<Integer> safetyInstructionNumber = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private List<Material> materials = new ArrayList<>();
    private List<TechnologicalTransition> transitions = new ArrayList<>();

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getWorkerCode() {
        return workerCode;
    }

    public void setWorkerCode(String workerCode) {
        this.workerCode = workerCode;
    }

    public String getWorkerCategory() {
        return workerCategory;
    }

    public void setWorkerCategory(String workerCategory) {
        this.workerCategory = workerCategory;
    }

    public List<Integer> getSafetyInstructionNumber() {
        return List.copyOf(safetyInstructionNumber);
    }

    public void setSafetyInstructionNumber(List<Integer> safetyInstructionNumber) {
        Objects.requireNonNull(safetyInstructionNumber);
        this.safetyInstructionNumber = safetyInstructionNumber;
    }

    public List<Product> getProducts() {
        return List.copyOf(products);
    }

    public void setProducts(List<Product> products) {
        Objects.requireNonNull(products);
        this.products = products;
    }

    public List<Material> getMaterials() {
        return List.copyOf(materials);
    }

    public void setMaterials(List<Material> materials) {
        Objects.requireNonNull(materials);
        this.materials = materials;
    }

    public List<TechnologicalTransition> getTransitions() {
        return List.copyOf(transitions);
    }

    public void setTransitions(List<TechnologicalTransition> transitions) {
        Objects.requireNonNull(transitions);
        this.transitions = transitions;
    }
}
