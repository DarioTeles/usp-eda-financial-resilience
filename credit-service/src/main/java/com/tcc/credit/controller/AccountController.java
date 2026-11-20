package com.tcc.credit.controller;

import com.tcc.credit.domain.model.Account;
import com.tcc.credit.service.CreditAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final CreditAccountService creditAccountService;
    private static final UUID DEFAULT_TARGET_ACCOUNT_ID = UUID.fromString("b0000000-0000-0000-0000-000000000002");

    public AccountController(CreditAccountService creditAccountService) {
        this.creditAccountService = creditAccountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable UUID id) {
        return creditAccountService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reset")
    public ResponseEntity<Account> resetAccount(@RequestBody(required = false) Map<String, Object> body) {
        BigDecimal balance = BigDecimal.ZERO;
        UUID accountId = DEFAULT_TARGET_ACCOUNT_ID;

        if (body != null) {
            if (body.containsKey("accountId")) {
                accountId = UUID.fromString(body.get("accountId").toString());
            }
            if (body.containsKey("balance")) {
                balance = new BigDecimal(body.get("balance").toString());
            }
        }

        Account account = creditAccountService.resetAccount(accountId, balance);
        return ResponseEntity.ok(account);
    }
}
