package by.niruin.techprocess_service.model.technological_process;

import by.niruin.techprocess_service.domain.enums.BlankType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TechnologicalOperationDto(String number,
                                        String name,
                                        List<String> workerCodes,
                                        List<SafetyInstructionDto> safetyInstructions,
                                        Boolean isOnlyForMan,
                                        Integer area,
                                        Double weight,
                                        BlankType blankType,
                                        EquipmentDto equipment,
                                        List<PartDto> parts,
                                        List<MaterialDto> materials,
                                        List<TechnologicalTransitionDto> transitions,
                                        List<ReviewCommentDto> reviewComments,
                                        List<SketchDto> sketches,
                                        @JsonProperty("sertified")
                                        Boolean isSertified,
                                        String operationType) {
}
