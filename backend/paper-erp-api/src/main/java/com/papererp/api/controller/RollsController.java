package com.papererp.api.controller;

import com.papererp.common.tenant.TenantContext;
import com.papererp.core.dto.RollGenealogyDto;
import com.papererp.core.entity.JumboRoll;
import com.papererp.core.entity.SlitRoll;
import com.papererp.core.repository.JumboRollRepository;
import com.papererp.core.repository.SlitRollRepository;
import com.papererp.core.service.SlittingKernelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rolls")
public class RollsController {

    private final SlittingKernelService slittingKernelService;
    private final JumboRollRepository jumboRollRepository;
    private final SlitRollRepository slitRollRepository;

    public RollsController(
            SlittingKernelService slittingKernelService,
            JumboRollRepository jumboRollRepository,
            SlitRollRepository slitRollRepository) {
        this.slittingKernelService = slittingKernelService;
        this.jumboRollRepository = jumboRollRepository;
        this.slitRollRepository = slitRollRepository;
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @GetMapping("/genealogy/{identifier}")
    public ResponseEntity<RollGenealogyDto> getGenealogy(@PathVariable String identifier) {
        return ResponseEntity.ok(slittingKernelService.getRollGenealogy(identifier));
    }

    @GetMapping("/jumbos")
    public ResponseEntity<List<JumboRoll>> listJumboRolls() {
        return ResponseEntity.ok(jumboRollRepository.findAllByTenantIdOrderByManufacturedAtDesc(getTenantId()));
    }

    @GetMapping("/slit-rolls")
    public ResponseEntity<List<SlitRoll>> listSlitRolls() {
        return ResponseEntity.ok(slitRollRepository.findAllByTenantIdOrderByCreatedAtDesc(getTenantId()));
    }
}
