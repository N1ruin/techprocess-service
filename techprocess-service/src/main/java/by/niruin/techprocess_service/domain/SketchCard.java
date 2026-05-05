package by.niruin.techprocess_service.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SketchCard {
    private List<Integer> operationNumbers = new ArrayList<>();
    private String fileStorageId;

    public List<Integer> getOperationNumbers() {
        return List.copyOf(operationNumbers);
    }

    public void setOperationNumbers(List<Integer> operationNumbers) {
        Objects.requireNonNull(operationNumbers);
        this.operationNumbers = operationNumbers;
    }

    public String getFileStorageId() {
        return fileStorageId;
    }

    public void setFileStorageId(String fileStorageId) {
        this.fileStorageId = fileStorageId;
    }

    public void addOperationNumber(Integer operationNumber) {
        if (operationNumber != null && !this.operationNumbers.contains(operationNumber)) {
            this.operationNumbers.add(operationNumber);
            Collections.sort(operationNumbers);
        }
    }

    public void removeOperationNumber(Integer operationNumber) {
        if (operationNumber != null) {
            this.operationNumbers.remove(operationNumber);
        }
    }
}
