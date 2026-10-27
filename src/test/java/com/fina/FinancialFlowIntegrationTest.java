package com.fina;

import com.fina.api.dto.AnalyticsSummary;
import com.fina.api.dto.CreateAccountRequest;
import com.fina.api.dto.CreateInvoiceRequest;
import com.fina.api.dto.TrialBalance;
import com.fina.model.Account;
import com.fina.model.BackupRecord;
import com.fina.model.BackupStatus;
import com.fina.model.Invoice;
import com.fina.model.InvoiceStatus;
import com.fina.model.Transaction;
import com.fina.service.AccountService;
import com.fina.service.AnalyticsService;
import com.fina.service.BackupService;
import com.fina.service.InvoiceService;
import com.fina.service.LedgerService;
import com.fina.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class FinancialFlowIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("fina.backup.enabled", () -> "false");
    }

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private RedissonClient redissonClient;

    @BeforeEach
    void clearRedis() {
        redissonClient.getKeys().flushdb();
    }

    @Test
    void endToEndFinancialFlow() {
        Account source = accountService.create(
                new CreateAccountRequest("Source", "source@example.com", new BigDecimal("100.00"))
        );
        Account destination = accountService.create(
                new CreateAccountRequest("Destination", "destination@example.com", BigDecimal.ZERO)
        );

        Transaction deposit = transactionService.deposit(source.id(), new BigDecimal("50.00"), "salary");
        assertEquals(0, new BigDecimal("150.00").compareTo(accountService.get(source.id()).balance()));

        List<Transaction> transfer = transactionService.transfer(
                source.id(), destination.id(), new BigDecimal("70.00"), "rent"
        );
        assertEquals(2, transfer.size());
        assertEquals(0, new BigDecimal("80.00").compareTo(accountService.get(source.id()).balance()));
        assertEquals(0, new BigDecimal("70.00").compareTo(accountService.get(destination.id()).balance()));

        transactionService.withdraw(source.id(), new BigDecimal("30.00"), "groceries");
        assertEquals(0, new BigDecimal("50.00").compareTo(accountService.get(source.id()).balance()));
        assertEquals(3, transactionService.history(source.id()).size());

        AnalyticsSummary summary = analyticsService.summary();
        assertEquals(2, summary.accounts());
        assertEquals(4, summary.transactions());
        assertEquals(0, new BigDecimal("120.00").compareTo(summary.totalBalance()));
        assertEquals(0, new BigDecimal("120.00").compareTo(summary.totalDeposits()));
        assertEquals(0, new BigDecimal("100.00").compareTo(summary.totalWithdrawals()));
        assertEquals(0, new BigDecimal("50.00").compareTo(analyticsService.forAccount(source.id()).balance()));

        Invoice invoice = invoiceService.create(new CreateInvoiceRequest(
                source.id(),
                "Acme",
                "billing@acme.example",
                new BigDecimal("20.00"),
                LocalDate.now().plusDays(7)
        ));
        Invoice paid = invoiceService.pay(invoice.id());
        assertEquals(InvoiceStatus.PAID, paid.status());

        Invoice overdue = invoiceService.create(new CreateInvoiceRequest(
                source.id(),
                "Old Customer",
                "old@example.com",
                new BigDecimal("10.00"),
                LocalDate.now().minusDays(1)
        ));
        invoiceService.markOverdueInvoices();
        assertEquals(InvoiceStatus.OVERDUE, invoiceService.get(overdue.id()).status());
        assertEquals(0, new BigDecimal("10.00").compareTo(
                analyticsService.forAccount(source.id()).outstandingInvoiceAmount()
        ));

        BackupRecord backup = backupService.trigger();
        assertEquals(BackupStatus.SUCCESS, backup.status());
        assertTrue(backup.message().contains("BGSAVE"));

        TrialBalance trialBalance = ledgerService.trialBalance();
        assertTrue(trialBalance.balanced());
        assertTrue(trialBalance.equationBalanced());
        assertEquals(0, trialBalance.assets().compareTo(
                trialBalance.liabilities().add(trialBalance.equity())
        ));
        assertEquals(14, ledgerService.entries(null).size());
    }

    @Test
    void concurrentWithdrawalsDoNotLoseUpdates() throws Exception {
        Account account = accountService.create(
                new CreateAccountRequest("Concurrent", "concurrent@example.com", new BigDecimal("100.00"))
        );

        int attempts = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < attempts; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    transactionService.withdraw(account.id(), new BigDecimal("10.00"), "concurrent");
                    return null;
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(0, BigDecimal.ZERO.compareTo(accountService.get(account.id()).balance()));
        assertEquals(attempts, transactionService.history(account.id()).size());
        assertEquals(attempts, analyticsService.forAccount(account.id()).transactionCount());
    }
}
