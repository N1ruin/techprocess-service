package by.niruin.techprocess_service.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ReviewComment {
    private String author;
    private String content;
    private Instant createdDate;
}
