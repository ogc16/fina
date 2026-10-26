package com.fina.service;

import com.fina.api.dto.AccountAnalytics;
import com.fina.api.dto.AnalyticsSummary;
import com.fina.model.Account;
import com.fina.model.AccountStatus;
import com.fina.model.Invoice;
import com.fina.model.InvoiceStatus;
import com.fina.model.Transaction;
import com.fina.model.TransactionStatus;
import com.fina.model.TransactionType;
import com.fina.repository.AccountRepository;
import com.fina.repository.InvoiceRepository;
import com.fina.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(accountRepository, transactionRepository, invoiceRepository);
    }

    @Test
    void summaryAggregatesBalancesTransactionsAndInvoices() {
        when(accountRepository.findAll()).thenReturn(List.of(
                account("a1", "100.00"),
                account("a2", "50.00")
        ));
        when(transactionRepository.findAll()).thenReturn(List.of(
                transaction("t1", "a1", "40.00", TransactionType.DEPOSIT),
                transaction("t2", "a1", "15.00", TransactionType.WITHDRAWAL),
                transaction("t3", "a2", "5.00", TransactionType.TRANSFER_IN)
        ));
        when(invoiceRepository.findAll()).thenReturn(List.of(
                invoice("i1", "a1", "25.00", InvoiceStatus.PENDING),
                invoice("i2", "a2", "10.00", InvoiceStatus.PAID),
                invoice("i3", "a1", "7.00", InvoiceStatus.OVERDUE)
        ));

        AnalyticsSummary summary = analyticsService.summary();

        assertEquals(2, summary.accounts());
        assertEquals(3, summary.transactions());
        assertEquals(3, summary.invoices());
        assertEquals(0, new BigDecimal("150.00").compareTo(summary.totalBalance()));
        assertEquals(0, new BigDecimal("45.00").compareTo(summary.totalDeposits()));
        assertEquals(0, new BigDecimal("15.00").compareTo(summary.totalWithdrawals()));
        assertEquals(0, new BigDecimal("32.00").compareTo(summary.outstandingInvoiceAmount()));
    }

    @Test
    void accountAnalyticsCountsOnlyMatchingTransactions() {
        when(accountRepository.findById("a1")).thenReturn(Optional.of(account("a1", "100.00")));
        when(transactionRepository.findByAccountId("a1")).thenReturn(List.of(
                transaction("t1", "a1", "40.00", TransactionType.DEPOSIT),
                transaction("t2", "a1", "15.00", TransactionType.TRANSFER_OUT)
        ));
        when(invoiceRepository.findByAccountId("a1")).thenReturn(List.of(
                invoice("i1", "a1", "25.00", InvoiceStatus.PENDING)
        ));

        AccountAnalytics analytics = analyticsService.forAccount("a1");

        assertEquals("a1", analytics.accountId());
        assertEquals(0, new BigDecimal("100.00").compareTo(analytics.balance()));
        assertEquals(0, new BigDecimal("40.00").compareTo(analytics.deposits()));
        assertEquals(0, new BigDecimal("15.00").compareTo(analytics.withdrawals()));
        assertEquals(2, analytics.transactionCount());
        assertEquals(0, new BigDecimal("25.00").compareTo(analytics.outstandingInvoiceAmount()));
    }

    private Account account(String id, String balance) {
        Instant now = Instant.now();
        return new Account(id, "Owner " + id, id + "@example.com", new BigDecimal(balance),
                AccountStatus.ACTIVE, now, now);
    }

    private Transaction transaction(String id, String accountId, String amount, TransactionType type) {
        return new Transaction(id, accountId, null, new BigDecimal(amount), type,
                TransactionStatus.COMPLETED, "test", Instant.now());
    }

    private Invoice invoice(String id, String accountId, String amount, InvoiceStatus status) {
        return new Invoice(id, accountId, "Customer", "customer@example.com", new BigDecimal(amount),
                LocalDate.now(), LocalDate.now().plusDays(7), status, Instant.now());
    }
}
