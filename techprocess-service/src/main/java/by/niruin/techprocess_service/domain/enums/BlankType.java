package by.niruin.techprocess_service.domain.enums;

public enum BlankType {
    ROUTE_BLANK_TITLE("Маршрутная карта",
            "ГОСТ 3.1118-82",
            "форма 1",
            BlankRole.TITLE,
            BlankFormat.A4),
    ROUTE_BLANK_CONTINUATION("Маршрутная карта",
            "ГОСТ 3.1118-82",
            "форма 1б",
            BlankRole.CONTINUATION,
            BlankFormat.A4),
    SKETCH_BLANK_TITLE_A4("Карта эскизов",
            "ГОСТ 3.1105-84",
            "форма 7",
            BlankRole.TITLE,
            BlankFormat.A4),
    SKETCH_BLANK_TITLE_A3("Карта эскизов",
            "ГОСТ 3.1105-84",
            "форма 7а",
            BlankRole.TITLE,
            BlankFormat.A3),
    SKETCH_BLANK_CONTINUATION_A4("Карта эскизов",
            "ГОСТ 3.1105-84",
            "форма 8",
            BlankRole.CONTINUATION,
            BlankFormat.A4),
    SKETCH_BLANK_CONTINUATION_A3("Карта эскизов",
            "ГОСТ 3.1105-84",
            "форма 8а",
            BlankRole.CONTINUATION,
            BlankFormat.A3),
    OPERATION_BLANK_TITLE("Операционная карта",
            "ГОСТ 3.1407-86",
            "форма 1",
            BlankRole.TITLE,
            BlankFormat.A4),
    OPERATION_BLANK_CONTINUATION("Операционная карта",
            "ГОСТ 3.1407-86",
            "форма 1а",
            BlankRole.CONTINUATION,
            BlankFormat.A4),
    CONTROL_BLANK_TITLE("Карта технического контроля",
            "ГОСТ 3.1407-86",
            "форма 1",
            BlankRole.TITLE,
            BlankFormat.A4),
    CONTROL_BLANK_CONTINUATION("Карта технического контроля",
            "ГОСТ 3.1407-86",
            "форма 1а",
            BlankRole.CONTINUATION,
            BlankFormat.A4);

    private final String displayName;
    private final String gost;
    private final String form;
    private final BlankRole role;
    private final BlankFormat format;

    BlankType(String displayName, String gost, String form, BlankRole role, BlankFormat format) {
        this.displayName = displayName;
        this.gost = gost;
        this.form = form;
        this.role = role;
        this.format = format;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGost() {
        return gost;
    }

    public String getForm() {
        return form;
    }

    public BlankRole getRole() {
        return role;
    }

    public BlankFormat getFormat() {
        return format;
    }

    public boolean isSketchMap() {
        return this == SKETCH_BLANK_TITLE_A4
                || this == SKETCH_BLANK_TITLE_A3
                || this == SKETCH_BLANK_CONTINUATION_A4
                || this == SKETCH_BLANK_CONTINUATION_A3;
    }

    public boolean isRouteMap() {
        return this == ROUTE_BLANK_TITLE
                || this == ROUTE_BLANK_CONTINUATION;
    }

    public boolean isOperationMap() {
        return this == OPERATION_BLANK_TITLE
                || this == OPERATION_BLANK_CONTINUATION;
    }

    public boolean isTitle() {
        return role == BlankRole.TITLE;
    }

    public boolean isContinuation() {
        return role == BlankRole.CONTINUATION;
    }
}
