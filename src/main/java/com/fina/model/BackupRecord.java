package com.fina.model;

import java.time.Instant;

public record BackupRecord(
        String id,
        Instant startedAt,
        Instant completedAt,
        BackupStatus status,
        String message,
        String filePath
) {
}
