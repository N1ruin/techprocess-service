package by.niruin.techprocess_service.config;

import by.niruin.techprocess_service.kafka.OutboxRecordProducer;
import by.niruin.techprocess_service.scheduler.OutboxRecordScheduler;
import by.niruin.techprocess_service.service.TransactionOutboxService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@TestConfiguration
@EnableConfigurationProperties(SchedulingOutboxProperties.class)
public class SchedulerConfig {
    @Bean
    public OutboxRecordScheduler outboxRecordScheduler(OutboxRecordProducer outboxRecordProducer,
                                                       TransactionOutboxService outboxService, SchedulingOutboxProperties properties) {
        return new OutboxRecordScheduler(outboxRecordProducer, outboxService, properties);
    }
}
