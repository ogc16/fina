package com.fina.api;

import com.fina.api.dto.TrialBalance;
import com.fina.model.LedgerEntry;
import com.fina.service.LedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/trial-balance")
    public TrialBalance trialBalance() {
        return ledgerService.trialBalance();
    }

    @GetMapping("/entries")
    public List<LedgerEntry> entries(@RequestParam(required = false) String accountId) {
        return ledgerService.entries(accountId);
    }
}
