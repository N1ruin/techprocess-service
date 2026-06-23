package by.niruin.techprocess_service.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.AccessType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnologicalTransition {
    @Setter
    @Getter
    private Integer number;
    @Setter
    @Getter
    private String content;
    private final List<Equipment> equipments = new ArrayList<>();
    @Getter
    @Setter
    private Boolean isSertified;

    public List<Equipment> getEquipments() {
        return Collections.unmodifiableList(equipments);
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setEquipments(List<Equipment> equipments) {
        this.equipments.clear();

        if (equipments != null) {
            this.equipments.addAll(equipments);
        }
    }
}
