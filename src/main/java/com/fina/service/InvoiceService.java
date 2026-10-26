package com.fina.service;

import com.fina.api.dto.CreateInvoiceRequest;
import com.fina.exception.ConflictException;
import com.fina.exception.ResourceNotFoundException;
import com.fina.model.Account;
import com.fina.model.Invoice;
import com.fina.model.InvoiceStatus;
import com.fina.repository.AccountRepository;
import com.fina.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final AccountRepository accountRepository;
    private final LockService lockService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            AccountRepository accountRepository,
            LockService lockService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.accountRepository = accountRepository;
        this.lockService = lockService;
    }

    public Invoice create(CreateInvoiceRequest request) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountId()));

        Invoice invoice = new Invoice(
                UUID.randomUUID().toString(),
                account.id(),
                request.customerName().trim(),
                request.customerEmail().trim().toLowerCase(),
                request.amount().setScale(2, RoundingMode.HALF_EVEN),
                LocalDate.now(),
                request.dueOn(),
                InvoiceStatus.PENDING,
                Instant.now()
        );
        return invoiceRepository.save(invoice);
    }

    public Invoice get(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    public List<Invoice> list() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> listForAccount(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account not found: " + accountId);
        }
        return invoiceRepository.findByAccountId(accountId);
    }

    public Invoice pay(String id) {
        return lockService.withLock("fina:lock:invoice:" + id, () -> {
            Invoice invoice = get(id);
            if (invoice.status() == InvoiceStatus.PAID) {
                throw new ConflictException("Invoice is already paid: " + id);
            }
            return invoiceRepository.save(invoice.withStatus(InvoiceStatus.PAID));
        });
    }

    @Scheduled(cron = "${fina.invoice.overdue-cron:0 5 0 * * *}")
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        int updated = 0;
        for (Invoice invoice : invoiceRepository.findAll()) {
            if (invoice.status() == InvoiceStatus.PENDING && invoice.dueOn().isBefore(today)) {
                invoiceRepository.save(invoice.withStatus(InvoiceStatus.OVERDUE));
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Marked {} invoice(s) as overdue", updated);
        }
    }

    public BigDecimal outstandingAmount(String accountId) {
        return invoiceRepository.findByAccountId(accountId).stream()
                .filter(invoice -> invoice.status() != InvoiceStatus.PAID)
                .map(Invoice::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
