package com.fina.service;

import com.fina.exception.ConflictException;
import com.fina.exception.InsufficientFundsException;
import com.fina.model.Account;
import com.fina.model.AccountStatus;
import com.fina.model.Transaction;
import com.fina.model.TransactionStatus;
import com.fina.model.TransactionType;
import com.fina.repository.AccountRepository;
import com.fina.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final LockService DIRECT_LOCKS = new LockService() {
        @Override
        public <T> T withLock(String key, Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T withLocks(Collection<String> keys, Supplier<T> action) {
            return action.get();
        }
    };

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountRepository, transactionRepository, DIRECT_LOCKS);
        lenient().when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void depositUpdatesBalanceAndRecordsTransaction() {
        when(accountRepository.findById("account-1"))
                .thenReturn(Optional.of(account("account-1", "100.00", AccountStatus.ACTIVE)));

        Transaction transaction = transactionService.deposit("account-1", new BigDecimal("25.50"), "salary");

        assertEquals(TransactionType.DEPOSIT, transaction.type());
        assertEquals(TransactionStatus.COMPLETED, transaction.status());
        assertEquals(0, new BigDecimal("25.50").compareTo(transaction.amount()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("125.50").compareTo(captor.getValue().balance()));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        when(accountRepository.findById("account-1"))
                .thenReturn(Optional.of(account("account-1", "10.00", AccountStatus.ACTIVE)));

        assertThrows(
                InsufficientFundsException.class,
                () -> transactionService.withdraw("account-1", new BigDecimal("20.00"), "rent")
        );

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdrawRejectsInactiveAccount() {
        when(accountRepository.findById("account-1"))
                .thenReturn(Optional.of(account("account-1", "100.00", AccountStatus.CLOSED)));

        assertThrows(
                ConflictException.class,
                () -> transactionService.withdraw("account-1", new BigDecimal("10.00"), "rent")
        );

        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferMovesFundsAndWritesBothEntries() {
        when(accountRepository.findById("source"))
                .thenReturn(Optional.of(account("source", "100.00", AccountStatus.ACTIVE)));
        when(accountRepository.findById("destination"))
                .thenReturn(Optional.of(account("destination", "50.00", AccountStatus.ACTIVE)));

        List<Transaction> transactions = transactionService.transfer(
                "source", "destination", new BigDecimal("30.00"), "rent"
        );

        assertEquals(2, transactions.size());
        assertEquals(TransactionType.TRANSFER_OUT, transactions.get(0).type());
        assertEquals(TransactionType.TRANSFER_IN, transactions.get(1).type());
        assertEquals("destination", transactions.get(0).counterpartyAccountId());
        assertEquals("source", transactions.get(1).counterpartyAccountId());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());

        Account savedSource = captor.getAllValues().stream()
                .filter(account -> account.id().equals("source"))
                .findFirst()
                .orElseThrow();
        Account savedDestination = captor.getAllValues().stream()
                .filter(account -> account.id().equals("destination"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("70.00").compareTo(savedSource.balance()));
        assertEquals(0, new BigDecimal("80.00").compareTo(savedDestination.balance()));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferRejectsSameAccount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transfer("same", "same", new BigDecimal("10.00"), "nope")
        );

        verify(accountRepository, never()).save(any());
    }

    private Account account(String id, String balance, AccountStatus status) {
        Instant now = Instant.now();
        return new Account(id, "Owner " + id, id + "@example.com", new BigDecimal(balance), status, now, now);
    }
}
