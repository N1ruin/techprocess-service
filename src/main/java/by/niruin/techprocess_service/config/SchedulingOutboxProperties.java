package by.niruin.techprocess_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "scheduler.outbox")
public class SchedulingOutboxProperties {
    private Integer batchSize;
    private Integer millisDelay;
}
