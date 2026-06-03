package by.niruin.techprocess_service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TechnologicalTransition {
    @Setter
    @Getter
    private Integer number;
    @Setter
    @Getter
    private String content;
    private final List<EquipmentReference> equipmentReferences = new ArrayList<>();
    @JsonProperty("sertified")
    private Boolean isSertified;

    public List<EquipmentReference> getEquipmentReferences() {
        return Collections.unmodifiableList(equipmentReferences);
    }

    public void setEquipmentReferences(List<EquipmentReference> equipmentReferences) {
        Objects.requireNonNull(equipmentReferences);
        this.equipmentReferences.clear();
        this.equipmentReferences.addAll(equipmentReferences);
    }
}
