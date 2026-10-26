package com.fina.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String id,
        String accountId,
        String counterpartyAccountId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String description,
        Instant timestamp
) {
}
