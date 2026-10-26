package com.papererp.api.controller;

import com.papererp.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Operations & Health", description = "فحص جاهزية النظام والخدمات التشغيلية")
public class HealthController {

    private final Instant startTime = Instant.now();

    @GetMapping("/health")
    @Operation(summary = "فحص سلامة النظام", description = "فحص آمن لحالة جاهزية النظام دون الكشف عن أي تفاصيل سرية أو بيئية داخلية")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> healthData = Map.of(
                "status", "UP",
                "service", "PaperERP Enterprise Backend",
                "version", "1.0.0",
                "timestamp", Instant.now(),
                "uptimeSeconds", uptimeMs / 1000
        );

        return ResponseEntity.ok(ApiResponse.ok("System is healthy and operational", healthData));
    }
}
