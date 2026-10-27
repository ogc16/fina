package com.fina.service;

import com.fina.metrics.FinaMetrics;
import com.fina.model.BackupRecord;
import com.fina.model.BackupStatus;
import com.fina.repository.BackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final BackupRepository backupRepository;
    private final StringRedisTemplate redisTemplate;
    private final FinaMetrics metrics;
    private final boolean enabled;
    private final String directory;
    private final String redisDumpPath;

    public BackupService(
            BackupRepository backupRepository,
            StringRedisTemplate redisTemplate,
            FinaMetrics metrics,
            @Value("${fina.backup.enabled:true}") boolean enabled,
            @Value("${fina.backup.directory:}") String directory,
            @Value("${fina.backup.redis-dump-path:dump.rdb}") String redisDumpPath
    ) {
        this.backupRepository = backupRepository;
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
        this.enabled = enabled;
        this.directory = directory;
        this.redisDumpPath = redisDumpPath;
    }

    @Scheduled(cron = "${fina.backup.cron:0 0 * * * *}")
    public void scheduledBackup() {
        if (!enabled) {
            return;
        }
        try {
            BackupRecord record = trigger();
            log.info("Scheduled backup {} finished with status {}", record.id(), record.status());
        } catch (Exception exception) {
            log.error("Scheduled backup failed", exception);
        }
    }

    public BackupRecord trigger() {
        long startedAtNanos = System.nanoTime();
        Instant startedAt = Instant.now();
        String id = UUID.randomUUID().toString();
        backupRepository.save(new BackupRecord(
                id, startedAt, null, BackupStatus.RUNNING, "Backup started", null
        ));

        try {
            long previousLastSave = lastSaveEpochSeconds();
            bgSave();
            long completedAtEpoch = waitForLastSave(startedAt.getEpochSecond(), previousLastSave);
            String filePath = copyDumpFile();

            String message = "BGSAVE completed at epoch " + completedAtEpoch;
            if (filePath == null && directory != null && !directory.isBlank()) {
                message += "; dump file was not available at " + redisDumpPath;
            }
            BackupRecord completed = new BackupRecord(
                    id, startedAt, Instant.now(), BackupStatus.SUCCESS, message, filePath
            );
            backupRepository.save(completed);
            metrics.recordBackup(
                    BackupStatus.SUCCESS,
                    Duration.ofNanos(System.nanoTime() - startedAtNanos),
                    completed.completedAt()
            );
            return completed;
        } catch (Exception exception) {
            log.error("Backup {} failed", id, exception);
            BackupRecord failed = new BackupRecord(
                    id,
                    startedAt,
                    Instant.now(),
                    BackupStatus.FAILED,
                    exception.getMessage(),
                    null
            );
            backupRepository.save(failed);
            metrics.recordBackup(
                    BackupStatus.FAILED,
                    Duration.ofNanos(System.nanoTime() - startedAtNanos),
                    null
            );
            return failed;
        }
    }

    public List<BackupRecord> list() {
        return backupRepository.findAll();
    }

    public BackupRecord get(String id) {
        return backupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Backup not found: " + id));
    }

    private void bgSave() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().bgSave();
            return null;
        });
    }

    private long lastSaveEpochSeconds() {
        Long lastSave = redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.serverCommands().lastSave()
        );
        return lastSave == null ? 0L : lastSave;
    }

    private long waitForLastSave(long startedAtEpochSeconds, long previousLastSave) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            long lastSave = lastSaveEpochSeconds();
            if (lastSave >= startedAtEpochSeconds && lastSave >= previousLastSave) {
                return lastSave;
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("Timed out waiting for Redis background save to finish");
    }

    private String copyDumpFile() {
        if (directory == null || directory.isBlank()) {
            return null;
        }
        Path source = Path.of(redisDumpPath);
        if (!Files.exists(source)) {
            return null;
        }
        try {
            Path targetDirectory = Path.of(directory);
            Files.createDirectories(targetDirectory);
            Path target = targetDirectory.resolve("dump-" + FILE_TIMESTAMP.format(Instant.now()) + ".rdb");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toAbsolutePath().toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not copy Redis dump to " + directory, exception);
        }
    }
}
