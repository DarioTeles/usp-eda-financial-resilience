package com.tcc.origination.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.origination.messaging.event.*;
import com.tcc.origination.service.TransferSagaOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final TransferSagaOrchestratorService orchestratorService;
    private final ObjectMapper objectMapper;

    public SagaEventListener(TransferSagaOrchestratorService orchestratorService, ObjectMapper objectMapper) {
        this.orchestratorService = orchestratorService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TRANSFER_DEBITED, groupId = "origination-group")
    public void onAccountDebited(String message) {
        try {
            AccountDebitedEvent event = objectMapper.readValue(message, AccountDebitedEvent.class);
            log.info("Kafka Listener [transfer-debited]: Saga [{}]", event.sagaId());
            orchestratorService.handleAccountDebited(event);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do tópico transfer-debited: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_DEBIT_FAILED, groupId = "origination-group")
    public void onDebitFailed(String message) {
        try {
            DebitFailedEvent event = objectMapper.readValue(message, DebitFailedEvent.class);
            log.warn("Kafka Listener [debit-failed]: Saga [{}]", event.sagaId());
            orchestratorService.handleDebitFailed(event);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do tópico debit-failed: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TRANSFER_CREDITED, groupId = "origination-group")
    public void onAccountCredited(String message) {
        try {
            AccountCreditedEvent event = objectMapper.readValue(message, AccountCreditedEvent.class);
            log.info("Kafka Listener [transfer-credited]: Saga [{}]", event.sagaId());
            orchestratorService.handleAccountCredited(event);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do tópico transfer-credited: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_CREDIT_FAILED, groupId = "origination-group")
    public void onCreditFailed(String message) {
        try {
            CreditFailedEvent event = objectMapper.readValue(message, CreditFailedEvent.class);
            log.warn("Kafka Listener [credit-failed]: Saga [{}]", event.sagaId());
            orchestratorService.handleCreditFailed(event);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do tópico credit-failed: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaTopicConfig.TOPIC_TRANSFER_COMPENSATED, groupId = "origination-group")
    public void onDebitCompensated(String message) {
        try {
            DebitCompensatedEvent event = objectMapper.readValue(message, DebitCompensatedEvent.class);
            log.info("Kafka Listener [transfer-compensated]: Saga [{}]", event.sagaId());
            orchestratorService.handleDebitCompensated(event.sagaId());
        } catch (Exception e) {
            log.error("Erro ao processar mensagem do tópico transfer-compensated: {}", message, e);
        }
    }
}
