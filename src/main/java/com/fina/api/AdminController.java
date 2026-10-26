package com.fina.api;

import com.fina.model.BackupRecord;
import com.fina.service.BackupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
public class AdminController {

    private final BackupService backupService;

    public AdminController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BackupRecord triggerBackup() {
        return backupService.trigger();
    }

    @GetMapping
    public List<BackupRecord> list() {
        return backupService.list();
    }
}
