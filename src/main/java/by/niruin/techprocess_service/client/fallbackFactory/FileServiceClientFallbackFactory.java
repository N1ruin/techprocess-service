package by.niruin.techprocess_service.client.fallbackFactory;

import by.niruin.techprocess_service.client.FileServiceClient;
import by.niruin.techprocess_service.exception.FileUploadException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class FileServiceClientFallbackFactory implements FallbackFactory<FileServiceClient> {
    @Override
    public FileServiceClient create(Throwable cause) {
        return file -> {
            if (cause instanceof FeignException feignException) {
                throw new FileUploadException(
                        "File service error: " + feignException.getMessage(),
                        cause,
                        feignException.status());
            }
            throw new FileUploadException("File service is temporarily unavailable", cause, 500);
        };
    }
}
