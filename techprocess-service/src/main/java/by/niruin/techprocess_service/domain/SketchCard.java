package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.AccessType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SketchCard {
    @Setter
    @Getter
    private BlankType blankType;
    private List<Integer> operationNumbers = new ArrayList<>();
    @Setter
    @Getter
    private String fileName;
    @Setter
    @Getter
    private Integer sketchSheetNumber;

    public List<Integer> getOperationNumbers() {
        return Collections.unmodifiableList(operationNumbers);
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setOperationNumbers(List<Integer> operationNumbers) {
        Objects.requireNonNull(operationNumbers);
        this.operationNumbers = new ArrayList<>(operationNumbers);
        Collections.sort(this.operationNumbers);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SketchCard that = (SketchCard) o;
        return Objects.equals(operationNumbers, that.operationNumbers)
               && Objects.equals(fileName, that.fileName)
               && Objects.equals(sketchSheetNumber, that.sketchSheetNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationNumbers, fileName, sketchSheetNumber);
    }
}
