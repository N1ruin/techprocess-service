package by.niruin.techprocess_service.client;

import by.niruin.techprocess_service.model.file.UploadFileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "file-service", path = "/api/v1/file-service/files/images", url = "${file-service.url}")
public interface FileServiceClient {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    UploadFileResponse uploadImage(@RequestPart("file") MultipartFile file);
}
