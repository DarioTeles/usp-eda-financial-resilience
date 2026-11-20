package com.tcc.credit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.credit.chaos.ChaosInjectionService;
import com.tcc.credit.domain.model.Account;
import com.tcc.credit.domain.repository.AccountRepository;
import com.tcc.credit.messaging.event.AccountCreditedEvent;
import com.tcc.credit.messaging.event.CreditFailedEvent;
import com.tcc.credit.outbox.OutboxEvent;
import com.tcc.credit.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditAccountService {

    private static final Logger log = LoggerFactory.getLogger(CreditAccountService.class);

    private final AccountRepository accountRepository;
    private final OutboxRepository outboxRepository;
    private final ChaosInjectionService chaosService;
    private final ObjectMapper objectMapper;

    public CreditAccountService(
            AccountRepository accountRepository,
            OutboxRepository outboxRepository,
            ChaosInjectionService chaosService,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.outboxRepository = outboxRepository;
        this.chaosService = chaosService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processCredit(UUID sagaId, UUID targetAccountId, BigDecimal amount) {
        log.info("Processando crédito para Saga [{}]: conta destino [{}], valor [{}]", sagaId, targetAccountId, amount);

        // 1. Aplicação do motor de Caos / Injeção de Falhas
        chaosService.applyChaosIfEnabled();

        // 2. Localização ou inicialização da conta destino
        Account account = accountRepository.findById(targetAccountId)
                .orElseGet(() -> new Account(targetAccountId, "ACC-TARGET-002", BigDecimal.ZERO));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Crédito efetuado com sucesso na conta [{}]. Novo saldo: [{}]", targetAccountId, newBalance);

        // 3. Gravação do evento de sucesso na Outbox
        AccountCreditedEvent event = new AccountCreditedEvent(sagaId, targetAccountId, amount);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "ACCOUNT",
                sagaId.toString(),
                "AccountCreditedEvent",
                serialize(event)
        );
        outboxRepository.save(outboxEvent);
        log.info("AccountCreditedEvent registrado na Outbox para Saga [{}]", sagaId);
    }

    @Transactional
    public void recordCreditFailure(UUID sagaId, UUID targetAccountId, BigDecimal amount, String reason) {
        log.warn("Registrando falha definitiva de crédito para Saga [{}]: motivo={}", sagaId, reason);

        CreditFailedEvent event = new CreditFailedEvent(sagaId, targetAccountId, amount, reason);
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "ACCOUNT",
                sagaId.toString(),
                "CreditFailedEvent",
                serialize(event)
        );
        outboxRepository.save(outboxEvent);
        log.info("CreditFailedEvent registrado na Outbox para publicação e compensação");
    }

    @Transactional
    public Account resetAccount(UUID id, BigDecimal balance) {
        Account account = accountRepository.findById(id)
                .orElseGet(() -> new Account(id, "ACC-TARGET-002", balance));
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
            throw new RuntimeException("Erro ao serializar evento para outbox", e);
        }
    }
}
