package by.niruin.techprocess_service.exception;

public class EntityAlreadyExistedException extends RuntimeException {
    public EntityAlreadyExistedException(String message) {
        super(message);
    }
}
