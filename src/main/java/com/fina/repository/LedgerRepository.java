package com.fina.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fina.model.LedgerEntry;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * Append-only repository for posted ledger entries.
 *
 * <p>There are intentionally no update or delete methods. Once a ledger entry is
 * posted it is immutable; corrections must be made with a new reversing journal.
 */
@Repository
public class LedgerRepository {

    private static final String KEY = "fina:ledger:entries";

    private final RMap<String, LedgerEntry> entries;

    public LedgerRepository(
            RedissonClient redissonClient,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.entries = redissonClient.getMap(
                KEY, new TypedJsonJacksonCodec(String.class, LedgerEntry.class, objectMapper)
        );
    }

    public LedgerEntry append(LedgerEntry entry) {
        LedgerEntry existing = entries.putIfAbsent(entry.id(), entry);
        if (existing != null) {
            throw new IllegalStateException("Ledger entry has already been posted: " + entry.id());
        }
        return entry;
    }

    public boolean journalExists(String journalId) {
        return entries.values().stream()
                .anyMatch(entry -> journalId.equals(entry.journalId()));
    }

    public List<LedgerEntry> findByJournalId(String journalId) {
        return entries.values().stream()
                .filter(entry -> journalId.equals(entry.journalId()))
                .sorted(entryOrder())
                .toList();
    }

    public List<LedgerEntry> findByAccountId(String accountId) {
        return entries.values().stream()
                .filter(entry -> accountId.equals(entry.accountId()))
                .sorted(entryOrder())
                .toList();
    }

    public List<LedgerEntry> findAll() {
        return entries.values().stream()
                .sorted(entryOrder())
                .toList();
    }

    private Comparator<LedgerEntry> entryOrder() {
        return Comparator.comparing(LedgerEntry::postedAt)
                .thenComparing(LedgerEntry::id);
    }
}
