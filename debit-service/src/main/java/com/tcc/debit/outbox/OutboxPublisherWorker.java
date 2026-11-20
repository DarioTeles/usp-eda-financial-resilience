package com.tcc.debit.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPublisherWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherWorker.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherWorker(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Debit Outbox worker encontrou {} eventos pendentes", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            String topic = resolveTopic(event.getEventType());
            if (topic == null) {
                log.error("Tópico desconhecido para o tipo de evento [{}], ID [{}]", event.getEventType(), event.getId());
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
                continue;
            }

            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Evento Debit Outbox [{}] tipo [{}] publicado com sucesso no tópico [{}]",
                        event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Erro ao publicar evento Debit Outbox [{}] no tópico [{}]: {}",
                        event.getId(), topic, e.getMessage());
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "AccountDebitedEvent" -> "transfer-debited";
            case "DebitFailedEvent" -> "debit-failed";
            case "DebitCompensatedEvent" -> "transfer-compensated";
            default -> null;
        };
    }
}
