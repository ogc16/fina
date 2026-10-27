package com.fina.service;

import com.fina.api.dto.TrialBalance;
import com.fina.metrics.FinaMetrics;
import com.fina.model.LedgerAccountType;
import com.fina.model.LedgerEntry;
import com.fina.model.LedgerLine;
import com.fina.model.LedgerSide;
import com.fina.repository.LedgerRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(
                ledgerRepository,
                new FinaMetrics(new SimpleMeterRegistry(), true)
        );
    }

    @Test
    void depositPostsBalancedDebitAndCreditEntries() {
        ledgerService.postDeposit("txn-1", "account-1", new BigDecimal("10.006"), "salary");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(2)).append(captor.capture());

        LedgerEntry debit = captor.getAllValues().get(0);
        LedgerEntry credit = captor.getAllValues().get(1);

        assertEquals(LedgerSide.DEBIT, debit.side());
        assertEquals(LedgerService.CASH_ACCOUNT_ID, debit.accountId());
        assertEquals(LedgerAccountType.ASSET, debit.accountType());

        assertEquals(LedgerSide.CREDIT, credit.side());
        assertEquals("account-1", credit.accountId());
        assertEquals(LedgerAccountType.LIABILITY, credit.accountType());

        assertEquals(0, new BigDecimal("10.01").compareTo(debit.amount()));
        assertEquals(0, debit.amount().compareTo(credit.amount()));
        assertEquals("txn-1", debit.journalId());
        assertEquals("txn-1", credit.journalId());
    }

    @Test
    void transferDebitsDestinationAndCreditsSource() {
        ledgerService.postTransfer("txn-2", "source", "destination", new BigDecimal("25.00"), "rent");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(2)).append(captor.capture());

        assertEquals("destination", captor.getAllValues().get(0).accountId());
        assertEquals(LedgerSide.DEBIT, captor.getAllValues().get(0).side());
        assertEquals("source", captor.getAllValues().get(1).accountId());
        assertEquals(LedgerSide.CREDIT, captor.getAllValues().get(1).side());
    }

    @Test
    void rejectsUnbalancedJournal() {
        List<LedgerLine> lines = List.of(
                LedgerLine.debit(LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET, new BigDecimal("10.00")),
                LedgerLine.credit("account-1", LedgerAccountType.LIABILITY, new BigDecimal("9.00"))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerService.post("journal-1", "ref-1", "bad entry", lines)
        );

        verify(ledgerRepository, never()).append(any());
    }

    @Test
    void rejectsJournalThatWasAlreadyPosted() {
        when(ledgerRepository.journalExists("journal-1")).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> ledgerService.post(
                        "journal-1",
                        "ref-1",
                        "duplicate",
                        List.of(
                                LedgerLine.debit(LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET,
                                        new BigDecimal("10.00")),
                                LedgerLine.credit("account-1", LedgerAccountType.LIABILITY,
                                        new BigDecimal("10.00"))
                        )
                )
        );

        verify(ledgerRepository, never()).append(any());
    }

    @Test
    void trialBalanceEnforcesAccountingEquation() {
        when(ledgerRepository.findAll()).thenReturn(List.of(
                entry("opening-debit", LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.DEBIT, "100.00"),
                entry("opening-credit", "account-1", LedgerAccountType.LIABILITY,
                        LedgerSide.CREDIT, "100.00"),
                entry("deposit-debit", LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.DEBIT, "50.00"),
                entry("deposit-credit", "account-1", LedgerAccountType.LIABILITY,
                        LedgerSide.CREDIT, "50.00"),
                entry("withdraw-debit", "account-1", LedgerAccountType.LIABILITY,
                        LedgerSide.DEBIT, "30.00"),
                entry("withdraw-credit", LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.CREDIT, "30.00"),
                entry("invoice-debit", LedgerService.RECEIVABLES_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.DEBIT, "20.00"),
                entry("invoice-credit", LedgerService.REVENUE_ACCOUNT_ID, LedgerAccountType.EQUITY,
                        LedgerSide.CREDIT, "20.00"),
                entry("payment-debit", LedgerService.CASH_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.DEBIT, "20.00"),
                entry("payment-credit", LedgerService.RECEIVABLES_ACCOUNT_ID, LedgerAccountType.ASSET,
                        LedgerSide.CREDIT, "20.00")
        ));

        TrialBalance trialBalance = ledgerService.trialBalance();

        assertTrue(trialBalance.balanced());
        assertTrue(trialBalance.equationBalanced());
        assertEquals(0, new BigDecimal("220.00").compareTo(trialBalance.totalDebits()));
        assertEquals(0, new BigDecimal("220.00").compareTo(trialBalance.totalCredits()));
        assertEquals(0, new BigDecimal("140.00").compareTo(trialBalance.assets()));
        assertEquals(0, new BigDecimal("120.00").compareTo(trialBalance.liabilities()));
        assertEquals(0, new BigDecimal("20.00").compareTo(trialBalance.equity()));
    }

    private LedgerEntry entry(
            String id,
            String accountId,
            LedgerAccountType accountType,
            LedgerSide side,
            String amount
    ) {
        return new LedgerEntry(
                id,
                "journal-" + id,
                "ref-" + id,
                accountId,
                accountType,
                side,
                new BigDecimal(amount),
                "test",
                Instant.parse("2026-10-26T08:00:00Z")
        );
    }
}
