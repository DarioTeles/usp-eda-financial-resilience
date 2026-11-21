package com.tcc.debit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.debit.messaging.event.CompensateDebitCommand;
import com.tcc.debit.messaging.event.TransferStartedEvent;
import com.tcc.debit.service.DebitAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class DebitEventListener {

    private static final Logger log = LoggerFactory.getLogger(DebitEventListener.class);

    private final DebitAccountService debitAccountService;
    private final ObjectMapper objectMapper;

    public DebitEventListener(DebitAccountService debitAccountService, ObjectMapper objectMapper) {
        this.debitAccountService = debitAccountService;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "transfer-started", groupId = "debit-group")
    public void onTransferStarted(String message) {
        try {
            log.info("Kafka Listener [transfer-started] recebido: {}", message);
            TransferStartedEvent event = objectMapper.readValue(message, TransferStartedEvent.class);
            debitAccountService.processDebit(
                    event.sagaId(),
                    event.sourceAccountId(),
                    event.targetAccountId(),
                    event.amount()
            );
        } catch (Exception e) {
            log.error("Erro ao processar transfer-started: {}", e.getMessage());
            throw new RuntimeException("Falha ao debitar conta na saga, disparando retentativa", e);
        }
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Mensagem enviada para Dead-Letter Topic [{}]: {}", topic, message);
    }

    @KafkaListener(topics = "transfer-compensate-debit", groupId = "debit-group")
    public void onCompensateDebit(String message) {
        try {
            log.info("Kafka Listener [transfer-compensate-debit] recebido: {}", message);
            CompensateDebitCommand command = objectMapper.readValue(message, CompensateDebitCommand.class);
            debitAccountService.processCompensation(
                    command.sagaId(),
                    command.sourceAccountId(),
                    command.amount()
            );
        } catch (Exception e) {
            log.error("Erro ao processar estorno compensatório: {}", e.getMessage(), e);
        }
    }
}
