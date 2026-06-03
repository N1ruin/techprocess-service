package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;
import lombok.Getter;
import lombok.Setter;

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

    public void setOperationNumbers(List<Integer> operationNumbers) {
        Objects.requireNonNull(operationNumbers);
        this.operationNumbers = new ArrayList<>(operationNumbers);
        Collections.sort(this.operationNumbers);
    }

    public void addOperationNumber(Integer operationNumber) {
        Objects.requireNonNull(operationNumber);

        if (!this.operationNumbers.contains(operationNumber)) {
            this.operationNumbers.add(operationNumber);
            Collections.sort(this.operationNumbers);
        }
    }

    public void removeOperationNumber(Integer operationNumber) {
        Objects.requireNonNull(operationNumber);
        this.operationNumbers.remove(operationNumber);
    }

    public boolean isTitle() {
        return blankType != null && blankType.isTitle();
    }

    public boolean isContinuation() {
        return blankType != null && blankType.isContinuation();
    }
}
