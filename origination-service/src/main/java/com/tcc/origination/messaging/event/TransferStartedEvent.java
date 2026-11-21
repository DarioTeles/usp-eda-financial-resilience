package com.tcc.origination.messaging.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferStartedEvent(
        UUID sagaId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount
) {}
