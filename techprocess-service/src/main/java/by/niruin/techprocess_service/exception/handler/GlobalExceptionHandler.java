package by.niruin.techprocess_service.exception.handler;

import by.niruin.techprocess_service.exception.EntityAlreadyExistException;
import by.niruin.techprocess_service.exception.EntityNotFoundException;
import by.niruin.techprocess_service.exception.TechprocessCancellationException;
import by.niruin.techprocess_service.exception.TechprocessSavingException;
import by.niruin.techprocess_service.model.error.ErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException exception) {
        log.warn("Exception: {}", exception.getMessage());

        var errorResponse = new ErrorResponse("Entity not found", exception.getMessage(),
                HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlready(EntityAlreadyExistException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity already exist", exception.getMessage(),
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
    public ResponseEntity<ErrorResponse> handleValidationException(TechprocessCancellationException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity cancellation exception", exception.getMessage(),
                HttpStatus.CONFLICT.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TechprocessSavingException.class)
    public ResponseEntity<ErrorResponse> handleSavingException(TechprocessSavingException exception) {
        log.warn("Exception: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Entity already exist", exception.getMessage(),
                HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unknown exception occurred: {}", exception.getMessage(), exception);

        var errorResponse = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
