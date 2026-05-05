package by.niruin.techprocess_service.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TechnologicalTransition {
    private Integer number;
    private String content;
    private List<String> tools = new ArrayList<>();

    public List<String> getTools() {
        return List.copyOf(tools);
    }

    public void setTools(List<String> tools) {
        Objects.requireNonNull(tools);
        this.tools = tools;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
