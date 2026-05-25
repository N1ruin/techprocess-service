package by.niruin.techprocess_service.domain;

import by.niruin.techprocess_service.domain.enums.BlankType;

import java.util.ArrayList;
import java.util.List;

public class TechnologicalOperation {
    private Integer operationNumber;
    private String operationName;
    private String workerCode;
    private final List<String> safetyInstructions = new ArrayList<>();
    private boolean onlyForWomen;
    private Integer sectionNumber;
    private String massDetail;
    private BlankType blankType;
    private String equipment;
    private final List<Product> products = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<TechnologicalTransition> transitions = new ArrayList<>();
    private boolean certified;
    private final List<SketchCard> sketchCards = new ArrayList<>();
}
