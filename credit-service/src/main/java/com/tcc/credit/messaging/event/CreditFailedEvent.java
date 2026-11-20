package com.tcc.credit.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditFailedEvent(
        UUID sagaId,
        UUID targetAccountId,
        BigDecimal amount,
        String reason
) {}
