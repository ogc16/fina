package com.fina.service;

import com.fina.api.dto.TrialBalance;
import com.fina.metrics.FinaMetrics;
import com.fina.model.LedgerAccountType;
import com.fina.model.LedgerEntry;
import com.fina.model.LedgerLine;
import com.fina.model.LedgerSide;
import com.fina.repository.LedgerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    public static final String CASH_ACCOUNT_ID = "system:cash";
    public static final String RECEIVABLES_ACCOUNT_ID = "system:accounts-receivable";
    public static final String REVENUE_ACCOUNT_ID = "system:revenue";

    private final LedgerRepository ledgerRepository;
    private final FinaMetrics metrics;

    public LedgerService(LedgerRepository ledgerRepository, FinaMetrics metrics) {
        this.ledgerRepository = ledgerRepository;
        this.metrics = metrics;
    }

    public void postAccountOpening(String accountId, BigDecimal amount) {
        post(
                accountId + ":opening",
                accountId,
                "Opening balance",
                List.of(
                        LedgerLine.debit(CASH_ACCOUNT_ID, LedgerAccountType.ASSET, amount),
                        LedgerLine.credit(accountId, LedgerAccountType.LIABILITY, amount)
                )
        );
    }

    public void postDeposit(String journalId, String accountId, BigDecimal amount, String description) {
        post(
                journalId,
                journalId,
                description,
                List.of(
                        LedgerLine.debit(CASH_ACCOUNT_ID, LedgerAccountType.ASSET, amount),
                        LedgerLine.credit(accountId, LedgerAccountType.LIABILITY, amount)
                )
        );
    }

    public void postWithdrawal(String journalId, String accountId, BigDecimal amount, String description) {
        post(
                journalId,
                journalId,
                description,
                List.of(
                        LedgerLine.debit(accountId, LedgerAccountType.LIABILITY, amount),
                        LedgerLine.credit(CASH_ACCOUNT_ID, LedgerAccountType.ASSET, amount)
                )
        );
    }

    public void postTransfer(
            String journalId,
            String sourceAccountId,
            String destinationAccountId,
            BigDecimal amount,
            String description
    ) {
        post(
                journalId,
                journalId,
                description,
                List.of(
                        LedgerLine.debit(destinationAccountId, LedgerAccountType.LIABILITY, amount),
                        LedgerLine.credit(sourceAccountId, LedgerAccountType.LIABILITY, amount)
                )
        );
    }

    public void postInvoice(String invoiceId, BigDecimal amount, String description) {
        post(
                invoiceId,
                invoiceId,
                description,
                List.of(
                        LedgerLine.debit(RECEIVABLES_ACCOUNT_ID, LedgerAccountType.ASSET, amount),
                        LedgerLine.credit(REVENUE_ACCOUNT_ID, LedgerAccountType.EQUITY, amount)
                )
        );
    }

    public void postInvoicePayment(String invoiceId, BigDecimal amount, String description) {
        post(
                invoiceId + ":payment",
                invoiceId,
                description,
                List.of(
                        LedgerLine.debit(CASH_ACCOUNT_ID, LedgerAccountType.ASSET, amount),
                        LedgerLine.credit(RECEIVABLES_ACCOUNT_ID, LedgerAccountType.ASSET, amount)
                )
        );
    }

    public void post(
            String journalId,
            String referenceId,
            String description,
            List<LedgerLine> lines
    ) {
        if (journalId == null || journalId.isBlank()) {
            throw new IllegalArgumentException("Journal id is required");
        }
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry requires at least two lines");
        }
        if (ledgerRepository.journalExists(journalId)) {
            throw new IllegalStateException("Journal has already been posted: " + journalId);
        }

        List<LedgerLine> normalized = lines.stream()
                .map(this::normalize)
                .toList();

        BigDecimal totalDebits = total(normalized, LedgerSide.DEBIT);
        BigDecimal totalCredits = total(normalized, LedgerSide.CREDIT);
        if (totalDebits.signum() <= 0 || totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    "Unbalanced journal entry: debits=" + totalDebits + ", credits=" + totalCredits
            );
        }

        Instant postedAt = Instant.now();
        for (LedgerLine line : normalized) {
            ledgerRepository.append(new LedgerEntry(
                    UUID.randomUUID().toString(),
                    journalId,
                    referenceId == null || referenceId.isBlank() ? journalId : referenceId,
                    line.accountId(),
                    line.accountType(),
                    line.side(),
                    line.amount(),
                    description == null ? "" : description.trim(),
                    postedAt
            ));
        }
        metrics.recordLedgerPost(normalized.size());
    }

    public List<LedgerEntry> entries(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return ledgerRepository.findAll();
        }
        return ledgerRepository.findByAccountId(accountId);
    }

    public TrialBalance trialBalance() {
        List<LedgerEntry> entries = ledgerRepository.findAll();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal assets = BigDecimal.ZERO;
        BigDecimal liabilities = BigDecimal.ZERO;
        BigDecimal equity = BigDecimal.ZERO;

        for (LedgerEntry entry : entries) {
            BigDecimal amount = entry.amount();
            if (entry.side() == LedgerSide.DEBIT) {
                totalDebits = totalDebits.add(amount);
            } else {
                totalCredits = totalCredits.add(amount);
            }

            BigDecimal signed = entry.side() == LedgerSide.DEBIT ? amount : amount.negate();
            switch (entry.accountType()) {
                case ASSET -> assets = assets.add(signed);
                case LIABILITY -> liabilities = liabilities.add(signed.negate());
                case EQUITY -> equity = equity.add(signed.negate());
            }
        }

        return new TrialBalance(
                totalDebits,
                totalCredits,
                assets,
                liabilities,
                equity,
                totalDebits.compareTo(totalCredits) == 0,
                assets.compareTo(liabilities.add(equity)) == 0
        );
    }

    private LedgerLine normalize(LedgerLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Ledger line is required");
        }
        if (line.accountId() == null || line.accountId().isBlank()) {
            throw new IllegalArgumentException("Ledger account id is required");
        }
        if (line.accountType() == null || line.side() == null) {
            throw new IllegalArgumentException("Ledger account type and side are required");
        }
        if (line.amount() == null || line.amount().signum() <= 0) {
            throw new IllegalArgumentException("Ledger amount must be greater than zero");
        }
        return new LedgerLine(
                line.accountId(),
                line.accountType(),
                line.side(),
                line.amount().setScale(2, RoundingMode.HALF_EVEN)
        );
    }

    private BigDecimal total(List<LedgerLine> lines, LedgerSide side) {
        return lines.stream()
                .filter(line -> line.side() == side)
                .map(LedgerLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
