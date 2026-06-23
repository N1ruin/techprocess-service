package by.niruin.techprocess_service.model.technological_process;

import lombok.Builder;

import java.util.List;

@Builder
public record SketchDto(String blankType,
                        List<Integer> operationNumbers,
                        String fileName,
                        Integer sketchSheetNumber) {
}
