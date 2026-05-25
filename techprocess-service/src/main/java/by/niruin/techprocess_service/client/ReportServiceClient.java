package by.niruin.techprocess_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "library-service", url = "${library.service.url}")
public interface ReportServiceClient {
    @GetMapping("/api/v1/report-service/reports/{archive-number}")
    TechnologicalProcessDto getReport(@PathVariable String archiveNumber);
}
