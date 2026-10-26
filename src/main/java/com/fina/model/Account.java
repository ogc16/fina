package com.fina.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Account(
        String id,
        String ownerName,
        String email,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Account withBalance(BigDecimal newBalance) {
        return new Account(id, ownerName, email, newBalance, status, createdAt, Instant.now());
    }

    public Account withStatus(AccountStatus newStatus) {
        return new Account(id, ownerName, email, balance, newStatus, createdAt, Instant.now());
    }
}
