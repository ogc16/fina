package com.fina.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fina.model.Invoice;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class InvoiceRepository {

    private static final String KEY = "fina:invoices";

    private final RMap<String, Invoice> invoices;

    public InvoiceRepository(
            RedissonClient redissonClient,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.invoices = redissonClient.getMap(
                KEY, new TypedJsonJacksonCodec(String.class, Invoice.class, objectMapper)
        );
    }

    public Invoice save(Invoice invoice) {
        invoices.put(invoice.id(), invoice);
        return invoice;
    }

    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(invoices.get(id));
    }

    public List<Invoice> findByAccountId(String accountId) {
        return invoices.values().stream()
                .filter(invoice -> accountId.equals(invoice.accountId()))
                .sorted(Comparator.comparing(Invoice::createdAt).reversed())
                .toList();
    }

    public List<Invoice> findAll() {
        return invoices.values().stream()
                .sorted(Comparator.comparing(Invoice::createdAt).reversed())
                .toList();
    }
}
