package com.fina.api;

import com.fina.api.dto.TransferRequest;
import com.fina.model.Transaction;
import com.fina.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransactionService transactionService;

    public TransferController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.description()
        );
    }
}
