package com.fina.metrics;

import com.fina.model.BackupStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FinaMetrics {

    private final MeterRegistry registry;
    private final AtomicLong lastSuccessfulBackup = new AtomicLong(0);

    public FinaMetrics(
            MeterRegistry registry,
            @Value("${fina.backup.enabled:true}") boolean backupEnabled
    ) {
        this.registry = registry;

        Gauge.builder("fina.backup.enabled", () -> backupEnabled ? 1 : 0)
                .description("Whether scheduled Redis backups are enabled")
                .register(registry);
        Gauge.builder("fina.backup.last.success.timestamp", lastSuccessfulBackup, AtomicLong::get)
                .description("Unix timestamp of the last successful Redis backup")
                .baseUnit("seconds")
                .register(registry);
    }

    public void recordTransaction(String operation, boolean success, Duration duration) {
        String result = success ? "success" : "failure";
        Counter.builder("fina.transactions")
                .description("Business transactions processed")
                .tag("operation", operation)
                .tag("result", result)
                .register(registry)
                .increment();
        Timer.builder("fina.transaction.duration")
                .description("Business transaction processing time")
                .tag("operation", operation)
                .tag("result", result)
                .register(registry)
                .record(duration);
    }

    public void recordLedgerPost(int entryCount) {
        Counter.builder("fina.ledger.journals")
                .description("Balanced journal entries posted")
                .register(registry)
                .increment();
        Counter.builder("fina.ledger.entries")
                .description("Immutable ledger lines posted")
                .register(registry)
                .increment(entryCount);
    }

    public void recordBackup(BackupStatus status, Duration duration, Instant completedAt) {
        String result = status.name().toLowerCase();
        Counter.builder("fina.backups")
                .description("Redis backup attempts")
                .tag("status", result)
                .register(registry)
                .increment();
        Timer.builder("fina.backup.duration")
                .description("Redis backup duration")
                .tag("status", result)
                .register(registry)
                .record(duration);
        if (status == BackupStatus.SUCCESS && completedAt != null) {
            lastSuccessfulBackup.set(completedAt.getEpochSecond());
        }
    }

    public double lastBackupSuccessEpochSeconds() {
        return lastSuccessfulBackup.get();
    }
}
