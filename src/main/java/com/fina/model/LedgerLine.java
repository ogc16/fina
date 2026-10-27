package com.fina.model;

import java.math.BigDecimal;

public record LedgerLine(
        String accountId,
        LedgerAccountType accountType,
        LedgerSide side,
        BigDecimal amount
) {
    public static LedgerLine debit(String accountId, LedgerAccountType accountType, BigDecimal amount) {
        return new LedgerLine(accountId, accountType, LedgerSide.DEBIT, amount);
    }

    public static LedgerLine credit(String accountId, LedgerAccountType accountType, BigDecimal amount) {
        return new LedgerLine(accountId, accountType, LedgerSide.CREDIT, amount);
    }
}
