package com.fina.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        String id,
        String journalId,
        String referenceId,
        String accountId,
        LedgerAccountType accountType,
        LedgerSide side,
        BigDecimal amount,
        String description,
        Instant postedAt
) {
}
