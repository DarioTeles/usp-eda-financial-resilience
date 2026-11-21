package com.tcc.origination.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountCreditedEvent(
        UUID sagaId,
        UUID targetAccountId,
        BigDecimal amount
) {}
