package by.niruin.techprocess_service.config;

import by.niruin.techprocess_service.model.event.EventType;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Profile("test")
public class KafkaConfig {
    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));
    }

    @Bean
    public NewTopic fileDeletionTopic() {
        return TopicBuilder.name(EventType.FILE_DELETED_EVENT.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic techprocessTopic() {
        return TopicBuilder.name(EventType.TECHNOLOGICAL_PROCESS_CREATED.getTopicName())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
