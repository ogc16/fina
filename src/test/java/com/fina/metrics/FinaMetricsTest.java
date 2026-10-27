package com.fina.metrics;

import com.fina.model.BackupStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinaMetricsTest {

    private SimpleMeterRegistry registry;
    private FinaMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new FinaMetrics(registry, true);
    }

    @Test
    void recordsTransactionCountersAndTimers() {
        metrics.recordTransaction("deposit", true, Duration.ofMillis(15));
        metrics.recordTransaction("withdraw", false, Duration.ofMillis(5));

        assertEquals(1.0, registry.get("fina.transactions")
                .tag("operation", "deposit")
                .tag("result", "success")
                .counter()
                .count());
        assertEquals(1.0, registry.get("fina.transactions")
                .tag("operation", "withdraw")
                .tag("result", "failure")
                .counter()
                .count());
        assertEquals(1, registry.get("fina.transaction.duration")
                .tag("operation", "deposit")
                .timer()
                .count());
    }

    @Test
    void recordsBackupOutcomeAndLastSuccessTimestamp() {
        Instant completedAt = Instant.parse("2026-10-26T08:00:00Z");

        metrics.recordBackup(BackupStatus.SUCCESS, Duration.ofSeconds(2), completedAt);
        metrics.recordBackup(BackupStatus.FAILED, Duration.ofSeconds(1), null);

        assertEquals(1.0, registry.get("fina.backups")
                .tag("status", "success")
                .counter()
                .count());
        assertEquals(1.0, registry.get("fina.backups")
                .tag("status", "failed")
                .counter()
                .count());
        assertEquals(completedAt.getEpochSecond(), metrics.lastBackupSuccessEpochSeconds());
        assertEquals(1.0, registry.get("fina.backup.enabled").gauge().value());
    }
}
