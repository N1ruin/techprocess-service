package by.niruin.techprocess_service.domain;

public class EquipmentReference {
    private String name;
    private String index;
    private String standard;
    private boolean isFromLibrary;

    public String getName() {
        return name;
    }

    public String getIndex() {
        return index;
    }

    public String getStandard() {
        return standard;
    }

    public boolean isFromLibrary() {
        return isFromLibrary;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public void setFromLibrary(boolean fromLibrary) {
        isFromLibrary = fromLibrary;
    }
}
