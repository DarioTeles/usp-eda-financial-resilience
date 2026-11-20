package com.tcc.debit.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDebitedEvent(
        UUID sagaId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount
) {}
