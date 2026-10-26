package com.fina.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fina.model.Transaction;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private static final String KEY = "fina:transactions";

    private final RMap<String, Transaction> transactions;

    public TransactionRepository(
            RedissonClient redissonClient,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.transactions = redissonClient.getMap(
                KEY, new TypedJsonJacksonCodec(String.class, Transaction.class, objectMapper)
        );
    }

    public Transaction save(Transaction transaction) {
        transactions.put(transaction.id(), transaction);
        return transaction;
    }

    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(transactions.get(id));
    }

    public List<Transaction> findByAccountId(String accountId) {
        return transactions.values().stream()
                .filter(transaction -> accountId.equals(transaction.accountId()))
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .toList();
    }

    public List<Transaction> findAll() {
        return transactions.values().stream()
                .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                .toList();
    }
}
