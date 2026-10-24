package com.papererp.api;

import com.papererp.common.exception.BusinessException;
import com.papererp.common.tenant.TenantContext;
import com.papererp.core.dto.RollGenealogyDto;
import com.papererp.core.dto.SlittingExecutionRequest;
import com.papererp.core.dto.SlittingExecutionResponse;
import com.papererp.core.entity.JumboRoll;
import com.papererp.core.repository.JumboRollRepository;
import com.papererp.core.repository.SlitRollRepository;
import com.papererp.core.service.SlittingKernelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlittingKernelServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SlittingKernelService slittingKernelService;

    @Autowired
    private JumboRollRepository jumboRollRepository;

    @Autowired
    private SlitRollRepository slitRollRepository;

    private UUID tenantId;
    private UUID plantId;
    private UUID gradeId;
    private UUID slitterId;
    private UUID operatorId;
    private JumboRoll savedJumbo;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM inventory_movements");
        jdbcTemplate.execute("DELETE FROM slit_rolls");
        jdbcTemplate.execute("DELETE FROM slitting_runs");
        jdbcTemplate.execute("DELETE FROM jumbo_rolls");
        jdbcTemplate.execute("DELETE FROM machines");
        jdbcTemplate.execute("DELETE FROM paper_grades");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM plants");
        jdbcTemplate.execute("DELETE FROM tenants");

        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        plantId = UUID.randomUUID();
        gradeId = UUID.randomUUID();
        slitterId = UUID.randomUUID();
        operatorId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO tenants (id, code, name_ar, name_en) VALUES (?, 'DEMO_MILL', 'مصنع الورق النموذجي', 'Demo Mill')", tenantId);
        jdbcTemplate.update("INSERT INTO plants (id, tenant_id, code, name_ar, name_en) VALUES (?, ?, 'PLANT_1', 'المصنع الرئيسي', 'Main Plant')", plantId, tenantId);
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, plant_id, username, password_hash, full_name_ar, role) VALUES (?, ?, ?, 'operator1', 'hash', 'مشغل المقص 1', 'PRODUCTION_OPERATOR')", operatorId, tenantId, plantId);
        jdbcTemplate.update("INSERT INTO paper_grades (id, tenant_id, code, name_ar, name_en, type, default_gsm_min, default_gsm_max) VALUES (?, ?, 'FLUT-120', 'فلوتنج 120', 'Fluting 120', 'FLUTING', 115.00, 125.00)", gradeId, tenantId);
        jdbcTemplate.update("INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, name_en, type, max_trim_width_mm) VALUES (?, ?, ?, 'SR-01', 'مقص الورق 1', 'Slitter 1', 'SLITTER_REWINDER', 2800.00)", slitterId, tenantId, plantId);

        // Insert a test mother Jumbo roll
        savedJumbo = new JumboRoll();
        savedJumbo.setTenantId(tenantId);
        savedJumbo.setPlantId(plantId);
        savedJumbo.setMachineId(slitterId);
        savedJumbo.setPaperGradeId(gradeId);
        savedJumbo.setSerialNumber("JR-2026-0001");
        savedJumbo.setTargetGsm(new BigDecimal("120.00"));
        savedJumbo.setDeckleWidthMm(new BigDecimal("2500.00"));
        savedJumbo.setGrossWeightKg(new BigDecimal("3550.00"));
        savedJumbo.setNetWeightKg(new BigDecimal("3500.00"));
        savedJumbo.setStatus("ACTIVE_WIP");
        savedJumbo.setManufacturedAt(OffsetDateTime.now());
        savedJumbo.setOperatorId(operatorId);
        savedJumbo = jumboRollRepository.save(savedJumbo);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should execute slitting atomically, enforce mass balance, create child rolls, and update jumbo status")
    void testSlittingExecutionSuccess() {
        SlittingExecutionRequest request = new SlittingExecutionRequest();
        request.setJumboRollId(savedJumbo.getId());
        request.setJumboSerialNumber(savedJumbo.getSerialNumber());
        request.setMachineId(slitterId);
        request.setOperatorId(operatorId);
        request.setTrimWasteKg(new BigDecimal("30.00"));

        // 3 child cuts: 1150kg + 1150kg + 1180kg + 30kg trim = 3510kg (variance = 10kg, 0.28% <= 1.0% tolerance)
        request.setCuts(List.of(
                new SlittingExecutionRequest.SlittingCutItem(1, new BigDecimal("800.00"), new BigDecimal("1200.00"), new BigDecimal("1150.00")),
                new SlittingExecutionRequest.SlittingCutItem(2, new BigDecimal("800.00"), new BigDecimal("1200.00"), new BigDecimal("1150.00")),
                new SlittingExecutionRequest.SlittingCutItem(3, new BigDecimal("850.00"), new BigDecimal("1200.00"), new BigDecimal("1180.00"))
        ));

        SlittingExecutionResponse response = slittingKernelService.executeSlittingRun(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getChildRolls()).hasSize(3);
        assertThat(response.getInputJumboWeightKg()).isEqualByComparingTo("3500.00");
        assertThat(response.getTotalChildWeightKg()).isEqualByComparingTo("3480.00");

        // Verify Jumbo status in DB updated to FULLY_SLIT
        JumboRoll updatedJumbo = jumboRollRepository.findByTenantIdAndId(tenantId, savedJumbo.getId()).orElseThrow();
        assertThat(updatedJumbo.getStatus()).isEqualTo("FULLY_SLIT");

        // Verify child rolls created in DB
        var childRolls = slitRollRepository.findByTenantIdAndParentJumboId(tenantId, savedJumbo.getId());
        assertThat(childRolls).hasSize(3);
    }

    @Test
    @DisplayName("Should reject slitting if mass balance variance exceeds 1.0% limit")
    void testSlittingRejectsVarianceExceedingLimit() {
        SlittingExecutionRequest request = new SlittingExecutionRequest();
        request.setJumboRollId(savedJumbo.getId());
        request.setJumboSerialNumber(savedJumbo.getSerialNumber());
        request.setMachineId(slitterId);
        request.setOperatorId(operatorId);
        request.setTrimWasteKg(new BigDecimal("20.00"));

        // Cuts sum to 2500kg + 20kg = 2520kg (Variance is 980kg or 28%, far above 1.0%)
        request.setCuts(List.of(
                new SlittingExecutionRequest.SlittingCutItem(1, new BigDecimal("1000.00"), new BigDecimal("1200.00"), new BigDecimal("2500.00"))
        ));

        assertThatThrownBy(() -> slittingKernelService.executeSlittingRun(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds 1.0% tolerance limit")
                .matches(e -> ((BusinessException) e).getErrorCode().equals("MASS_BALANCE_TOLERANCE_EXCEEDED"));
    }

    @Test
    @DisplayName("Should retrieve full genealogy of child roll linking back to mother jumbo")
    void testRollGenealogyTraceability() {
        // First run slitting
        SlittingExecutionRequest request = new SlittingExecutionRequest();
        request.setJumboRollId(savedJumbo.getId());
        request.setJumboSerialNumber(savedJumbo.getSerialNumber());
        request.setMachineId(slitterId);
        request.setOperatorId(operatorId);
        request.setTrimWasteKg(new BigDecimal("20.00"));
        request.setCuts(List.of(
                new SlittingExecutionRequest.SlittingCutItem(1, new BigDecimal("1200.00"), new BigDecimal("1200.00"), new BigDecimal("1740.00")),
                new SlittingExecutionRequest.SlittingCutItem(2, new BigDecimal("1250.00"), new BigDecimal("1200.00"), new BigDecimal("1740.00"))
        ));

        SlittingExecutionResponse runResp = slittingKernelService.executeSlittingRun(request);
        String childBarcode = runResp.getChildRolls().get(0).getBarcode();

        // Trace genealogy
        RollGenealogyDto genealogy = slittingKernelService.getRollGenealogy(childBarcode);

        assertThat(genealogy).isNotNull();
        assertThat(genealogy.getRollType()).isEqualTo("SLIT");
        assertThat(genealogy.getParentJumboSerial()).isEqualTo("JR-2026-0001");
        assertThat(genealogy.getRelatedRolls()).hasSize(2);
        assertThat(genealogy.getMovementHistory()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return network and LAN info from System endpoint")
    void testSystemNetworkEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/system/network-info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverStatus").value("ONLINE"))
                .andExpect(jsonPath("$.multiTenancyMode").value("COMPOSITE_RLS_ZERO_TRUST"));
    }
}
