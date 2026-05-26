package by.niruin.techprocess_service.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalTransition {
    private Integer number;
    private String content;
    private final List<EquipmentReference> equipmentReferences = new ArrayList<>();

    public Integer getNumber() {
        return number;
    }

    public String getContent() {
        return content;
    }

    public List<EquipmentReference> getEquipmentReferences() {
        return Collections.unmodifiableList(equipmentReferences);
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setEquipmentReferences(List<EquipmentReference> equipmentReferences) {
        Objects.requireNonNull(equipmentReferences);
        this.equipmentReferences.clear();
        this.equipmentReferences.addAll(equipmentReferences);
    }
}
