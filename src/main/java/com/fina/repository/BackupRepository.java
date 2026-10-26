package com.fina.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fina.model.BackupRecord;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class BackupRepository {

    private static final String KEY = "fina:backups";

    private final RMap<String, BackupRecord> backups;

    public BackupRepository(
            RedissonClient redissonClient,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.backups = redissonClient.getMap(
                KEY, new TypedJsonJacksonCodec(String.class, BackupRecord.class, objectMapper)
        );
    }

    public BackupRecord save(BackupRecord record) {
        backups.put(record.id(), record);
        return record;
    }

    public Optional<BackupRecord> findById(String id) {
        return Optional.ofNullable(backups.get(id));
    }

    public List<BackupRecord> findAll() {
        return backups.values().stream()
                .sorted(Comparator.comparing(BackupRecord::startedAt).reversed())
                .toList();
    }
}
