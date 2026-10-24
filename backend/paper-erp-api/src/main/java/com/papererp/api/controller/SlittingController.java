package com.papererp.api.controller;

import com.papererp.common.tenant.TenantContext;
import com.papererp.core.dto.SlittingExecutionRequest;
import com.papererp.core.dto.SlittingExecutionResponse;
import com.papererp.core.entity.SlittingRun;
import com.papererp.core.repository.SlittingRunRepository;
import com.papererp.core.service.SlittingKernelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slitting")
public class SlittingController {

    private final SlittingKernelService slittingKernelService;
    private final SlittingRunRepository slittingRunRepository;

    public SlittingController(SlittingKernelService slittingKernelService, SlittingRunRepository slittingRunRepository) {
        this.slittingKernelService = slittingKernelService;
        this.slittingRunRepository = slittingRunRepository;
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @PostMapping("/execute")
    public ResponseEntity<SlittingExecutionResponse> executeSlitting(@RequestBody SlittingExecutionRequest request) {
        SlittingExecutionResponse response = slittingKernelService.executeSlittingRun(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/runs")
    public ResponseEntity<List<SlittingRun>> listRecentRuns() {
        return ResponseEntity.ok(slittingRunRepository.findAllByTenantIdOrderByStartedAtDesc(getTenantId()));
    }
}
