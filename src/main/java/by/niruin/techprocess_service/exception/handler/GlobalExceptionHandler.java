package by.niruin.techprocess_service.exception.handler;

import by.niruin.techprocess_service.exception.*;
import by.niruin.techprocess_service.model.error.ErrorResponse;
import feign.FeignException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException exception) {
        log.warn("Exception: {}", exception.getMessage());

        var errorResponse = new ErrorResponse("Entity not found", "The requested techprocess does not exist" +
                " or has been removed", HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlready(EntityAlreadyExistException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity conflict", "A record with the provided data already exists",
                HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        var errorResponse = new ErrorResponse("Validation error", validationErrors, HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TechprocessCancellationException.class)
    public ResponseEntity<ErrorResponse> handleTechprocessCancellationException(TechprocessCancellationException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Techprocess cancellation exception", "Unable to cancel the " +
                "techprocess. Please try again", HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TechprocessSavingException.class)
    public ResponseEntity<ErrorResponse> handleSavingException(TechprocessSavingException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Save failed", "Unable to save the data. " +
                "Please check your input and try again", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(AuthorizationException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Access Denied", "You don't have permission to perform this action",
                HttpStatus.UNAUTHORIZED.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException exception) {
        int status = exception.status() >= 400 ? exception.status() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        if (exception.responseBody().isPresent()) {
            try {
                log.info("OpenFeign exception. {}", parseFeignException(exception));
                var errorResponse = new ErrorResponse("File service error", "File uploading error." +
                        " Please, try again later",
                        HttpStatus.CONFLICT.value());
                return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(status));
            } catch (Exception e) {
                log.info("Parsing JSON error from feign exception", e);
            }
        }

        var fallbackResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(fallbackResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TechprocessUpdatingException.class)
    public ResponseEntity<ErrorResponse> handleTechprocessUpdatingException(TechprocessUpdatingException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Techprocess updating exception: ", "Unable to update the " +
                "resource. Please try again", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("File upload error", "Unable to upload the file." +
                " Please check the file format and size", exception.getHttpStatus());

        return ResponseEntity.status(exception.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.warn("Unknown exception occurred: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AuthorizationDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());

        var errorResponse = new ErrorResponse("Forbidden",
                "Access Denied",
                HttpStatus.FORBIDDEN.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    private ErrorResponse parseFeignException(FeignException exception) {
        byte[] rawBody = exception.responseBody().get().array();

        return objectMapper.readValue(rawBody, ErrorResponse.class);
    }
}
