package com.tcc.origination.controller;

import com.tcc.origination.domain.model.TransferSaga;
import com.tcc.origination.dto.CreateTransferRequest;
import com.tcc.origination.dto.TransferResponse;
import com.tcc.origination.service.TransferSagaOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferSagaOrchestratorService orchestratorService;

    public TransferController(TransferSagaOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferSaga saga = orchestratorService.initiateTransfer(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(TransferResponse.from(saga));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID id) {
        return orchestratorService.getTransfer(id)
                .map(saga -> ResponseEntity.ok(TransferResponse.from(saga)))
                .orElse(ResponseEntity.notFound().build());
    }
}
