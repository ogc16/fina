package com.fina.health;

import com.fina.model.BackupRecord;
import com.fina.model.BackupStatus;
import com.fina.repository.BackupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupHealthIndicatorTest {

    @Mock
    private BackupRepository backupRepository;

    @Test
    void reportsUpWhenBackupsAreDisabled() {
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                backupRepository, false, Duration.ofHours(2)
        );

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void reportsUnknownWhenNoBackupsExist() {
        when(backupRepository.findAll()).thenReturn(List.of());
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                backupRepository, true, Duration.ofHours(2)
        );

        assertEquals(Status.UNKNOWN, indicator.health().getStatus());
    }

    @Test
    void reportsDownWhenLatestBackupFailed() {
        when(backupRepository.findAll()).thenReturn(List.of(
                backup(BackupStatus.FAILED, Instant.now())
        ));
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                backupRepository, true, Duration.ofHours(2)
        );

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void reportsDownWhenLastSuccessIsStale() {
        when(backupRepository.findAll()).thenReturn(List.of(
                backup(BackupStatus.SUCCESS, Instant.now().minus(Duration.ofHours(3)))
        ));
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                backupRepository, true, Duration.ofHours(2)
        );

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void reportsUpWhenLastSuccessIsRecent() {
        when(backupRepository.findAll()).thenReturn(List.of(
                backup(BackupStatus.SUCCESS, Instant.now().minus(Duration.ofMinutes(5)))
        ));
        BackupHealthIndicator indicator = new BackupHealthIndicator(
                backupRepository, true, Duration.ofHours(2)
        );

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(true, health.getDetails().containsKey("lastSuccess"));
    }

    private BackupRecord backup(BackupStatus status, Instant completedAt) {
        return new BackupRecord(
                "backup-1",
                completedAt.minusSeconds(30),
                completedAt,
                status,
                "test",
                null
        );
    }
}
