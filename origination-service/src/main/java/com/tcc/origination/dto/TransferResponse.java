package com.tcc.origination.dto;

import com.tcc.origination.domain.model.SagaStatus;
import com.tcc.origination.domain.model.TransferSaga;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID sagaId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        SagaStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TransferResponse from(TransferSaga saga) {
        return new TransferResponse(
                saga.getId(),
                saga.getSourceAccountId(),
                saga.getTargetAccountId(),
                saga.getAmount(),
                saga.getStatus(),
                saga.getCreatedAt(),
                saga.getUpdatedAt()
        );
    }
}
