package com.fina.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountAnalytics(
        String accountId,
        BigDecimal balance,
        BigDecimal deposits,
        BigDecimal withdrawals,
        long transactionCount,
        BigDecimal outstandingInvoiceAmount,
        Instant generatedAt
) {
}
