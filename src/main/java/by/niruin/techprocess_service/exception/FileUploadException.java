package by.niruin.techprocess_service.exception;

import lombok.Getter;

@Getter
public class FileUploadException extends RuntimeException {
    private final int httpStatus;

    public FileUploadException(String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

}
