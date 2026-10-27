package com.fina.service;

import com.fina.api.dto.CreateAccountRequest;
import com.fina.exception.ResourceNotFoundException;
import com.fina.model.Account;
import com.fina.model.AccountStatus;
import com.fina.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LockService lockService;
    private final LedgerService ledgerService;

    public AccountService(
            AccountRepository accountRepository,
            LockService lockService,
            LedgerService ledgerService
    ) {
        this.accountRepository = accountRepository;
        this.lockService = lockService;
        this.ledgerService = ledgerService;
    }

    public Account create(CreateAccountRequest request) {
        Instant now = Instant.now();
        BigDecimal openingBalance = request.initialBalance() == null
                ? BigDecimal.ZERO
                : request.initialBalance().setScale(2, RoundingMode.HALF_EVEN);

        Account account = new Account(
                UUID.randomUUID().toString(),
                request.ownerName().trim(),
                request.email().trim().toLowerCase(),
                openingBalance,
                AccountStatus.ACTIVE,
                now,
                now
        );
        Account saved = accountRepository.save(account);
        if (openingBalance.signum() > 0) {
            ledgerService.postAccountOpening(saved.id(), openingBalance);
        }
        return saved;
    }

    public Account get(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public List<Account> list() {
        return accountRepository.findAll();
    }

    public Account close(String id) {
        return lockService.withLock(lockKey(id), () -> {
            Account account = get(id);
            if (account.status() == AccountStatus.CLOSED) {
                return account;
            }
            if (account.balance().compareTo(BigDecimal.ZERO) != 0) {
                throw new com.fina.exception.ConflictException(
                        "Account balance must be zero before closing"
                );
            }
            return accountRepository.save(account.withStatus(AccountStatus.CLOSED));
        });
    }

    public static String lockKey(String accountId) {
        return "fina:lock:account:" + accountId;
    }
}
