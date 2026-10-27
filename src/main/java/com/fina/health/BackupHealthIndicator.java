package com.fina.health;

import com.fina.model.BackupRecord;
import com.fina.model.BackupStatus;
import com.fina.repository.BackupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component("backupHealth")
public class BackupHealthIndicator implements HealthIndicator {

    private final BackupRepository backupRepository;
    private final boolean enabled;
    private final Duration maxAge;

    public BackupHealthIndicator(
            BackupRepository backupRepository,
            @Value("${fina.backup.enabled:true}") boolean enabled,
            @Value("${fina.backup.max-age:2h}") Duration maxAge
    ) {
        this.backupRepository = backupRepository;
        this.enabled = enabled;
        this.maxAge = maxAge;
    }

    @Override
    public Health health() {
        if (!enabled) {
            return Health.up().withDetail("enabled", false).build();
        }

        List<BackupRecord> records;
        try {
            records = backupRepository.findAll();
        } catch (RuntimeException exception) {
            return Health.down(exception).build();
        }

        if (records.isEmpty()) {
            return Health.unknown().withDetail("reason", "No backups recorded yet").build();
        }

        BackupRecord latest = records.getFirst();
        if (latest.status() == BackupStatus.FAILED) {
            return Health.down()
                    .withDetail("latestStatus", latest.status())
                    .withDetail("latestStartedAt", latest.startedAt())
                    .withDetail("message", latest.message())
                    .build();
        }

        Optional<Instant> lastSuccess = records.stream()
                .filter(record -> record.status() == BackupStatus.SUCCESS)
                .map(record -> record.completedAt() == null ? record.startedAt() : record.completedAt())
                .findFirst();

        if (lastSuccess.isEmpty()) {
            return Health.unknown()
                    .withDetail("reason", "No successful backup recorded yet")
                    .withDetail("latestStatus", latest.status())
                    .build();
        }

        long ageSeconds = Duration.between(lastSuccess.get(), Instant.now()).toSeconds();
        Health.Builder builder = ageSeconds > maxAge.toSeconds() ? Health.down() : Health.up();
        return builder
                .withDetail("lastSuccess", lastSuccess.get())
                .withDetail("ageSeconds", ageSeconds)
                .withDetail("maxAgeSeconds", maxAge.toSeconds())
                .build();
    }
}
