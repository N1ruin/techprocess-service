package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.MaterialUnit;

public class Part {
    private String position;
    private String name;
    private String number;
    private String supplierCode;
    private MaterialUnit materialUnit;
    private Integer quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public MaterialUnit getMaterialUnit() {
        return materialUnit;
    }

    public void setMaterialUnit(MaterialUnit materialUnit) {
        this.materialUnit = materialUnit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
