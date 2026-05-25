package by.niruin.techprocess_service.domain;

public class Material {
    private String position;
    private String name;
    private String supplierCode;
    private String standard;
    private MaterialUnit unit;
    private Integer rationingUnit;
    private Double consumptionRate;

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public MaterialUnit getUnit() {
        return unit;
    }

    public void setUnit(MaterialUnit unit) {
        this.unit = unit;
    }

    public Integer getRationingUnit() {
        return rationingUnit;
    }

    public void setRationingUnit(Integer rationingUnit) {
        this.rationingUnit = rationingUnit;
    }

    public Double getConsumptionRate() {
        return consumptionRate;
    }

    public void setConsumptionRate(Double consumptionRate) {
        this.consumptionRate = consumptionRate;
    }


}
