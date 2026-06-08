package by.niruin.techprocess_service.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "by.niruin.techprocess_service.client")
public class FeignClientConfig {
}
