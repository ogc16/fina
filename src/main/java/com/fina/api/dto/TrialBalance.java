package com.fina.api.dto;

import java.math.BigDecimal;

public record TrialBalance(
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal assets,
        BigDecimal liabilities,
        BigDecimal equity,
        boolean balanced,
        boolean equationBalanced
) {
}
