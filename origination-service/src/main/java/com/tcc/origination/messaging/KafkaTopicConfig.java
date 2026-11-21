package com.tcc.origination.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_TRANSFER_STARTED = "transfer-started";
    public static final String TOPIC_TRANSFER_DEBITED = "transfer-debited";
    public static final String TOPIC_DEBIT_FAILED = "debit-failed";
    public static final String TOPIC_TRANSFER_CREDITED = "transfer-credited";
    public static final String TOPIC_CREDIT_FAILED = "credit-failed";
    public static final String TOPIC_TRANSFER_COMPENSATE_DEBIT = "transfer-compensate-debit";
    public static final String TOPIC_TRANSFER_COMPENSATED = "transfer-compensated";

    public static final String TOPIC_TRANSFER_STARTED_DLT = "transfer-started-dlt";
    public static final String TOPIC_TRANSFER_DEBITED_DLT = "transfer-debited-dlt";
    public static final String TOPIC_TRANSFER_CREDITED_DLT = "transfer-credited-dlt";

    @Bean
    public NewTopic transferStartedTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_STARTED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferDebitedTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_DEBITED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic debitFailedTopic() {
        return TopicBuilder.name(TOPIC_DEBIT_FAILED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferCreditedTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_CREDITED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic creditFailedTopic() {
        return TopicBuilder.name(TOPIC_CREDIT_FAILED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferCompensateDebitTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_COMPENSATE_DEBIT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferCompensatedTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_COMPENSATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferStartedDltTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_STARTED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferDebitedDltTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_DEBITED_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transferCreditedDltTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_CREDITED_DLT).partitions(1).replicas(1).build();
    }
}
