package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SketchCard {
    private BlankType blankType;
    private List<Integer> operationNumbers = new ArrayList<>();
    private String fileName;
    private Integer sketchSheetNumber;

    public BlankType getBlankType() {
        return blankType;
    }

    public List<Integer> getOperationNumbers() {
        return Collections.unmodifiableList(operationNumbers);
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getSketchSheetNumber() {
        return sketchSheetNumber;
    }

    public void setBlankType(BlankType blankType) {
        this.blankType = blankType;
    }

    public void setOperationNumbers(List<Integer> operationNumbers) {
        Objects.requireNonNull(operationNumbers);
        this.operationNumbers = new ArrayList<>(operationNumbers);
        Collections.sort(this.operationNumbers);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setSketchSheetNumber(Integer sketchSheetNumber) {
        this.sketchSheetNumber = sketchSheetNumber;
    }

    public void addOperationNumber(Integer operationNumber) {
        if (operationNumber != null && !this.operationNumbers.contains(operationNumber)) {
            this.operationNumbers.add(operationNumber);
            Collections.sort(this.operationNumbers);
        }
    }

    public void removeOperationNumber(Integer operationNumber) {
        if (operationNumber != null) {
            this.operationNumbers.remove(operationNumber);
        }
    }

    public boolean isTitle() {
        return blankType != null && blankType.isTitle();
    }

    public boolean isContinuation() {
        return blankType != null && blankType.isContinuation();
    }
}
