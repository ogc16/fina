package com.fina.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsSummary(
        long accounts,
        long transactions,
        long invoices,
        BigDecimal totalBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal outstandingInvoiceAmount,
        Instant generatedAt
) {
}
