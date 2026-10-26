package com.fina.api;

import com.fina.api.dto.CreateAccountRequest;
import com.fina.api.dto.MoneyRequest;
import com.fina.model.Account;
import com.fina.model.Transaction;
import com.fina.service.AccountService;
import com.fina.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping
    public List<Account> list() {
        return accountService.list();
    }

    @GetMapping("/{id}")
    public Account get(@PathVariable String id) {
        return accountService.get(id);
    }

    @PostMapping("/{id}/deposit")
    public Transaction deposit(@PathVariable String id, @Valid @RequestBody MoneyRequest request) {
        return transactionService.deposit(id, request.amount(), request.description());
    }

    @PostMapping("/{id}/withdraw")
    public Transaction withdraw(@PathVariable String id, @Valid @RequestBody MoneyRequest request) {
        return transactionService.withdraw(id, request.amount(), request.description());
    }

    @GetMapping("/{id}/transactions")
    public List<Transaction> history(@PathVariable String id) {
        return transactionService.history(id);
    }

    @PostMapping("/{id}/close")
    public Account close(@PathVariable String id) {
        return accountService.close(id);
    }
}
