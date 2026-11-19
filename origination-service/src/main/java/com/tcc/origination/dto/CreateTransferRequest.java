package com.tcc.origination.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull(message = "sourceAccountId é obrigatório")
        UUID sourceAccountId,

        @NotNull(message = "targetAccountId é obrigatório")
        UUID targetAccountId,

        @NotNull(message = "amount é obrigatório")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero")
        BigDecimal amount
) {}
