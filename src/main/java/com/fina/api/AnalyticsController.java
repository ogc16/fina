package com.fina.api;

import com.fina.api.dto.AccountAnalytics;
import com.fina.api.dto.AnalyticsSummary;
import com.fina.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public AnalyticsSummary summary() {
        return analyticsService.summary();
    }

    @GetMapping("/accounts/{accountId}")
    public AccountAnalytics forAccount(@PathVariable String accountId) {
        return analyticsService.forAccount(accountId);
    }
}
