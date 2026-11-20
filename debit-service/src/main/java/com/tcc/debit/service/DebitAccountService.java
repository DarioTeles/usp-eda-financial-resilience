package com.tcc.debit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.debit.domain.model.Account;
import com.tcc.debit.domain.repository.AccountRepository;
import com.tcc.debit.messaging.event.AccountDebitedEvent;
import com.tcc.debit.messaging.event.DebitCompensatedEvent;
import com.tcc.debit.messaging.event.DebitFailedEvent;
import com.tcc.debit.outbox.OutboxEvent;
import com.tcc.debit.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class DebitAccountService {

    private static final Logger log = LoggerFactory.getLogger(DebitAccountService.class);

    private final AccountRepository accountRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public DebitAccountService(
            AccountRepository accountRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processDebit(UUID sagaId, UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) {
        log.info("Processando débito para Saga [{}]: conta origem [{}], valor [{}]", sagaId, sourceAccountId, amount);

        Optional<Account> optAccount = accountRepository.findById(sourceAccountId);
        if (optAccount.isEmpty()) {
            log.warn("Conta de origem [{}] não encontrada. Gerando DebitFailedEvent para Saga [{}]", sourceAccountId, sagaId);
            recordDebitFailure(sagaId, sourceAccountId, amount, "Conta de origem não encontrada");
            return;
        }

        Account account = optAccount.get();
        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Saldo insuficiente na conta [{}] (Saldo: {}, Débito: {}). Gerando DebitFailedEvent",
                    sourceAccountId, account.getBalance(), amount);
            recordDebitFailure(sagaId, sourceAccountId, amount, "Saldo insuficiente");
            return;
        }

        // Executar débito
        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Débito efetuado com sucesso na conta [{}]. Novo saldo: [{}]", sourceAccountId, newBalance);

        // Gravar AccountDebitedEvent na Outbox
        AccountDebitedEvent event = new AccountDebitedEvent(sagaId, sourceAccountId, targetAccountId, amount);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "ACCOUNT",
                sagaId.toString(),
                "AccountDebitedEvent",
                serialize(event)
        );
        outboxRepository.save(outboxEvent);
        log.info("AccountDebitedEvent registrado na Outbox para Saga [{}]", sagaId);
    }

    @Transactional
    public void processCompensation(UUID sagaId, UUID sourceAccountId, BigDecimal amount) {
        log.info("Processando estorno compensatório para Saga [{}]: conta origem [{}], valor [{}]",
                sagaId, sourceAccountId, amount);

        Account account = accountRepository.findById(sourceAccountId)
                .orElseGet(() -> new Account(sourceAccountId, "ACC-ORIGIN-001", BigDecimal.ZERO));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Estorno compensatório concluído na conta [{}]. Novo saldo: [{}]", sourceAccountId, newBalance);

        // Gravar DebitCompensatedEvent na Outbox
        DebitCompensatedEvent event = new DebitCompensatedEvent(sagaId, sourceAccountId, amount);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "ACCOUNT",
                sagaId.toString(),
                "DebitCompensatedEvent",
                serialize(event)
        );
        outboxRepository.save(outboxEvent);
        log.info("DebitCompensatedEvent registrado na Outbox para Saga [{}]", sagaId);
    }

    private void recordDebitFailure(UUID sagaId, UUID sourceAccountId, BigDecimal amount, String reason) {
        DebitFailedEvent event = new DebitFailedEvent(sagaId, sourceAccountId, amount, reason);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "ACCOUNT",
                sagaId.toString(),
                "DebitFailedEvent",
                serialize(event)
        );
        outboxRepository.save(outboxEvent);
    }

    @Transactional
    public Account resetAccount(UUID id, BigDecimal balance) {
        Account account = accountRepository.findById(id)
                .orElseGet(() -> new Account(id, "ACC-ORIGIN-001", balance));
        account.setBalance(balance);
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public Optional<Account> getAccount(UUID id) {
        return accountRepository.findById(id);
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar evento", e);
        }
    }
}
