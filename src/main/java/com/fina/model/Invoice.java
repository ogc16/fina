package com.fina.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record Invoice(
        String id,
        String accountId,
        String customerName,
        String customerEmail,
        BigDecimal amount,
        LocalDate issuedOn,
        LocalDate dueOn,
        InvoiceStatus status,
        Instant createdAt
) {
    public Invoice withStatus(InvoiceStatus newStatus) {
        return new Invoice(id, accountId, customerName, customerEmail, amount, issuedOn, dueOn, newStatus, createdAt);
    }
}
