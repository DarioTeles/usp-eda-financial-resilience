package com.tcc.debit.controller;

import com.tcc.debit.domain.model.Account;
import com.tcc.debit.service.DebitAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final DebitAccountService debitAccountService;
    private static final UUID DEFAULT_ORIGIN_ACCOUNT_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    public AccountController(DebitAccountService debitAccountService) {
        this.debitAccountService = debitAccountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable UUID id) {
        return debitAccountService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset")
    public ResponseEntity<Account> resetAccount(@RequestBody(required = false) Map<String, Object> body) {
        BigDecimal balance = BigDecimal.valueOf(10000.00);
        UUID accountId = DEFAULT_ORIGIN_ACCOUNT_ID;

        if (body != null) {
            if (body.containsKey("accountId")) {
                accountId = UUID.fromString(body.get("accountId").toString());
            }
            if (body.containsKey("balance")) {
                balance = new BigDecimal(body.get("balance").toString());
            }
        }

        Account account = debitAccountService.resetAccount(accountId, balance);
        return ResponseEntity.ok(account);
    }
}
