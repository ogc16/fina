package com.fina.service;

import com.fina.api.dto.AccountAnalytics;
import com.fina.api.dto.AnalyticsSummary;
import com.fina.exception.ResourceNotFoundException;
import com.fina.model.Account;
import com.fina.model.Invoice;
import com.fina.model.InvoiceStatus;
import com.fina.model.Transaction;
import com.fina.model.TransactionType;
import com.fina.repository.AccountRepository;
import com.fina.repository.InvoiceRepository;
import com.fina.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class AnalyticsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    public AnalyticsService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public AnalyticsSummary summary() {
        List<Account> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();
        List<Invoice> invoices = invoiceRepository.findAll();

        return new AnalyticsSummary(
                accounts.size(),
                transactions.size(),
                invoices.size(),
                accounts.stream().map(Account::balance).reduce(BigDecimal.ZERO, BigDecimal::add),
                totalIncoming(transactions),
                totalOutgoing(transactions),
                invoices.stream()
                        .filter(invoice -> invoice.status() != InvoiceStatus.PAID)
                        .map(Invoice::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                Instant.now()
        );
    }

    public AccountAnalytics forAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);

        return new AccountAnalytics(
                accountId,
                account.balance(),
                totalIncoming(transactions),
                totalOutgoing(transactions),
                transactions.size(),
                invoiceRepository.findByAccountId(accountId).stream()
                        .filter(invoice -> invoice.status() != InvoiceStatus.PAID)
                        .map(Invoice::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                Instant.now()
        );
    }

    private BigDecimal totalIncoming(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.DEPOSIT
                        || transaction.type() == TransactionType.TRANSFER_IN)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalOutgoing(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.WITHDRAWAL
                        || transaction.type() == TransactionType.TRANSFER_OUT)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
