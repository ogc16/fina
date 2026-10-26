package com.fina.api;

import com.fina.api.dto.CreateInvoiceRequest;
import com.fina.model.Invoice;
import com.fina.service.InvoiceService;
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
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.create(request);
    }

    @GetMapping
    public List<Invoice> list() {
        return invoiceService.list();
    }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable String id) {
        return invoiceService.get(id);
    }

    @GetMapping("/account/{accountId}")
    public List<Invoice> listForAccount(@PathVariable String accountId) {
        return invoiceService.listForAccount(accountId);
    }

    @PostMapping("/{id}/pay")
    public Invoice pay(@PathVariable String id) {
        return invoiceService.pay(id);
    }
}
