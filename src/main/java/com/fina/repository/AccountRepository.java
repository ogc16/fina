package com.fina.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fina.model.Account;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private static final String KEY = "fina:accounts";

    private final RMap<String, Account> accounts;

    public AccountRepository(
            RedissonClient redissonClient,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.accounts = redissonClient.getMap(
                KEY, new TypedJsonJacksonCodec(String.class, Account.class, objectMapper)
        );
    }

    public Account save(Account account) {
        accounts.put(account.id(), account);
        return account;
    }

    public Optional<Account> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public List<Account> findAll() {
        return accounts.values().stream()
                .sorted(Comparator.comparing(Account::createdAt))
                .toList();
    }

    public boolean existsById(String id) {
        return accounts.containsKey(id);
    }
}
