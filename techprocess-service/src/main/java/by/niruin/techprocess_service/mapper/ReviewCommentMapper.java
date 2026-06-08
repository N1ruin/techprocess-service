package by.niruin.techprocess_service.mapper;

import by.niruin.techprocess_service.domain.ReviewComment;
import by.niruin.techprocess_service.model.technological_process.AddCommentRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewCommentMapper {
    ReviewComment toComment(AddCommentRequest request);
}
