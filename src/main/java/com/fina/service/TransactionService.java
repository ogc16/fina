package com.fina.service;

import com.fina.exception.ConflictException;
import com.fina.exception.InsufficientFundsException;
import com.fina.exception.ResourceNotFoundException;
import com.fina.model.Account;
import com.fina.model.AccountStatus;
import com.fina.model.Transaction;
import com.fina.model.TransactionStatus;
import com.fina.model.TransactionType;
import com.fina.repository.AccountRepository;
import com.fina.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LockService lockService;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LockService lockService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.lockService = lockService;
    }

    public Transaction deposit(String accountId, BigDecimal amount, String description) {
        BigDecimal normalized = normalizeAmount(amount);
        return lockService.withLock(AccountService.lockKey(accountId), () -> {
            Account account = requireActiveAccount(accountId);
            accountRepository.save(account.withBalance(account.balance().add(normalized)));
            return record(accountId, null, normalized, TransactionType.DEPOSIT, description);
        });
    }

    public Transaction withdraw(String accountId, BigDecimal amount, String description) {
        BigDecimal normalized = normalizeAmount(amount);
        return lockService.withLock(AccountService.lockKey(accountId), () -> {
            Account account = requireActiveAccount(accountId);
            ensureFunds(account, normalized);
            accountRepository.save(account.withBalance(account.balance().subtract(normalized)));
            return record(accountId, null, normalized, TransactionType.WITHDRAWAL, description);
        });
    }

    public List<Transaction> transfer(
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            String description
    ) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }
        BigDecimal normalized = normalizeAmount(amount);

        return lockService.withLocks(
                List.of(AccountService.lockKey(fromAccountId), AccountService.lockKey(toAccountId)),
                () -> {
                    Account source = requireActiveAccount(fromAccountId);
                    Account destination = requireActiveAccount(toAccountId);
                    ensureFunds(source, normalized);

                    accountRepository.save(source.withBalance(source.balance().subtract(normalized)));
                    accountRepository.save(destination.withBalance(destination.balance().add(normalized)));

                    Transaction outgoing = record(
                            fromAccountId, toAccountId, normalized, TransactionType.TRANSFER_OUT, description
                    );
                    Transaction incoming = record(
                            toAccountId, fromAccountId, normalized, TransactionType.TRANSFER_IN, description
                    );
                    return List.of(outgoing, incoming);
                }
        );
    }

    public List<Transaction> history(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> list() {
        return transactionRepository.findAll();
    }

    public Transaction get(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
    }

    private Account requireActiveAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        if (account.status() != AccountStatus.ACTIVE) {
            throw new ConflictException("Account is not active: " + accountId);
        }
        return account;
    }

    private void ensureFunds(Account account, BigDecimal amount) {
        if (account.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: balance=" + account.balance() + ", requested=" + amount
            );
        }
    }

    private Transaction record(
            String accountId,
            String counterpartyAccountId,
            BigDecimal amount,
            TransactionType type,
            String description
    ) {
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                accountId,
                counterpartyAccountId,
                amount,
                type,
                TransactionStatus.COMPLETED,
                description == null ? "" : description.trim(),
                Instant.now()
        );
        return transactionRepository.save(transaction);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
