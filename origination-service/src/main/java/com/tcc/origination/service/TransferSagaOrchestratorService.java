package com.tcc.origination.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.origination.domain.model.SagaStatus;
import com.tcc.origination.domain.model.TransferSaga;
import com.tcc.origination.domain.repository.TransferSagaRepository;
import com.tcc.origination.dto.CreateTransferRequest;
import com.tcc.origination.messaging.event.*;
import com.tcc.origination.outbox.OutboxEvent;
import com.tcc.origination.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransferSagaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestratorService.class);

    private final TransferSagaRepository sagaRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public TransferSagaOrchestratorService(
            TransferSagaRepository sagaRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.sagaRepository = sagaRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransferSaga initiateTransfer(CreateTransferRequest request) {
        UUID sagaId = UUID.randomUUID();
        log.info("Iniciando TransferSaga [{}] da conta [{}] para a conta [{}] valor [{}]",
                sagaId, request.sourceAccountId(), request.targetAccountId(), request.amount());

        TransferSaga saga = new TransferSaga(
                sagaId,
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount(),
                SagaStatus.STARTED
        );
        sagaRepository.save(saga);

        TransferStartedEvent event = new TransferStartedEvent(
                sagaId,
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()
        );

        String payload = serialize(event);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "TRANSFER_SAGA",
                sagaId.toString(),
                "TransferStartedEvent",
                payload
        );
        outboxRepository.save(outboxEvent);

        log.info("TransferSaga [{}] persistida com status STARTED e evento TransferStartedEvent na Outbox", sagaId);
        return saga;
    }

    @Transactional
    public void handleAccountDebited(AccountDebitedEvent event) {
        log.info("Recebido AccountDebitedEvent para Saga [{}]", event.sagaId());
        Optional<TransferSaga> opt = sagaRepository.findById(event.sagaId());
        if (opt.isPresent()) {
            TransferSaga saga = opt.get();
            saga.setStatus(SagaStatus.DEBITED);
            sagaRepository.save(saga);
            log.info("Saga [{}] transicionada para status DEBITED", saga.getId());
        } else {
            log.warn("Saga [{}] não encontrada para AccountDebitedEvent", event.sagaId());
        }
    }

    @Transactional
    public void handleDebitFailed(DebitFailedEvent event) {
        log.warn("Recebido DebitFailedEvent para Saga [{}]: motivo={}", event.sagaId(), event.reason());
        Optional<TransferSaga> opt = sagaRepository.findById(event.sagaId());
        if (opt.isPresent()) {
            TransferSaga saga = opt.get();
            saga.setStatus(SagaStatus.FAILED);
            sagaRepository.save(saga);
            log.info("Saga [{}] transicionada para status FAILED (débito falhou)", saga.getId());
        }
    }

    @Transactional
    public void handleAccountCredited(AccountCreditedEvent event) {
        log.info("Recebido AccountCreditedEvent para Saga [{}]", event.sagaId());
        Optional<TransferSaga> opt = sagaRepository.findById(event.sagaId());
        if (opt.isPresent()) {
            TransferSaga saga = opt.get();
            saga.setStatus(SagaStatus.COMPLETED);
            sagaRepository.save(saga);
            log.info("Saga [{}] transicionada para status COMPLETED (transferência finalizada com sucesso)", saga.getId());
        }
    }

    @Transactional
    public void handleCreditFailed(CreditFailedEvent event) {
        log.warn("Recebido CreditFailedEvent para Saga [{}]: motivo={}. Disparando compensação...",
                event.sagaId(), event.reason());
        Optional<TransferSaga> opt = sagaRepository.findById(event.sagaId());
        if (opt.isPresent()) {
            TransferSaga saga = opt.get();
            saga.setStatus(SagaStatus.COMPENSATING);
            sagaRepository.save(saga);

            CompensateDebitCommand command = new CompensateDebitCommand(
                    saga.getId(),
                    saga.getSourceAccountId(),
                    saga.getAmount(),
                    event.reason()
            );
            String payload = serialize(command);
            OutboxEvent outboxEvent = new OutboxEvent(
                    UUID.randomUUID(),
                    "TRANSFER_SAGA",
                    saga.getId().toString(),
                    "CompensateDebitCommand",
                    payload
            );
            outboxRepository.save(outboxEvent);

            log.info("Saga [{}] transicionada para COMPENSATING e comando CompensateDebitCommand na Outbox", saga.getId());
        }
    }

    @Transactional
    public void handleDebitCompensated(UUID sagaId) {
        log.info("Recebida confirmação de compensação de débito para Saga [{}]", sagaId);
        Optional<TransferSaga> opt = sagaRepository.findById(sagaId);
        if (opt.isPresent()) {
            TransferSaga saga = opt.get();
            saga.setStatus(SagaStatus.FAILED);
            sagaRepository.save(saga);
            log.info("Saga [{}] finalizada com status FAILED após estorno compensatório bem-sucedido", sagaId);
        }
    }

    public Optional<TransferSaga> getTransfer(UUID id) {
        return sagaRepository.findById(id);
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar evento para outbox", e);
        }
    }
}
