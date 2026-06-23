package by.niruin.techprocess_service.model.technological_process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record TechnologicalTransitionDto(Integer number,
                                         String content,
                                         List<EquipmentDto> equipments,
                                         @JsonProperty("sertified")
                                         Boolean isSertified) {
}
