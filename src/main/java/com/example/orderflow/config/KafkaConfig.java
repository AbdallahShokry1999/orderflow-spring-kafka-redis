package com.example.orderflow.config;

import com.example.orderflow.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    public NewTopic ordersCreatedTopic() {
        return TopicBuilder.name(Topics.ORDERS_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ordersCreatedDeadLetterTopic() {
        return TopicBuilder.name(Topics.ORDERS_CREATED_DLT).partitions(1).replicas(1).build();
    }

    /**
     * Retry a failing record 3 more times (1s apart), then publish it to "<topic>.DLT"
     * instead of blocking the partition forever.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Delivery attempt {} failed for record key={}: {}",
                        deliveryAttempt, record.key(), ex.getMessage()));

        return errorHandler;
    }
}
