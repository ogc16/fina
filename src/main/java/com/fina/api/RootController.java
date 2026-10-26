package com.fina.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "fina",
                "description", "Financial Tracking & Analytics Suite",
                "status", "UP",
                "documentation", "https://github.com/ogc16/fina#readme",
                "endpoints", Map.of(
                        "health", "/actuator/health",
                        "accounts", "/api/accounts",
                        "transactions", "/api/transactions",
                        "transfers", "/api/transfers",
                        "invoices", "/api/invoices",
                        "analytics", "/api/analytics/summary",
                        "backups", "/api/admin/backups"
                )
        );
    }
}
