package com.tcc.origination.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CompensateDebitCommand(
        UUID sagaId,
        UUID sourceAccountId,
        BigDecimal amount,
        String reason
) {}
