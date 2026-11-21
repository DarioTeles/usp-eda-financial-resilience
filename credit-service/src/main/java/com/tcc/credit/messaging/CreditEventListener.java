package com.tcc.credit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.credit.messaging.event.AccountDebitedEvent;
import com.tcc.credit.service.CreditAccountService;
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
public class CreditEventListener {

    private static final Logger log = LoggerFactory.getLogger(CreditEventListener.class);

    private final CreditAccountService creditAccountService;
    private final ObjectMapper objectMapper;

    public CreditEventListener(CreditAccountService creditAccountService, ObjectMapper objectMapper) {
        this.creditAccountService = creditAccountService;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "transfer-debited", groupId = "credit-group")
    public void onTransferDebited(String message) {
        log.info("Kafka Listener [transfer-debited] recebido: {}", message);
        AccountDebitedEvent event;
        try {
            event = objectMapper.readValue(message, AccountDebitedEvent.class);
        } catch (Exception e) {
            log.error("Erro na desserialização do evento transfer-debited: {}", e.getMessage());
            throw new RuntimeException("Erro de desserialização", e);
        }

        // processCredit lançará ChaosSimulatedException caso o caos esteja ativado
        creditAccountService.processCredit(event.sagaId(), event.targetAccountId(), event.amount());
    }

    @DltHandler
    public void handleDlt(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Mensagem direcionada ao Dead-Letter Topic (DLT) [{}]: {}", topic, message);
        try {
            AccountDebitedEvent event = objectMapper.readValue(message, AccountDebitedEvent.class);
            creditAccountService.recordCreditFailure(
                    event.sagaId(),
                    event.targetAccountId(),
                    event.amount(),
                    "Falha irrecuperável no crédito após retentativas exponenciais (DLT)"
            );
        } catch (Exception e) {
            log.error("Erro no processamento do DLT Handler: {}", e.getMessage(), e);
        }
    }
}
