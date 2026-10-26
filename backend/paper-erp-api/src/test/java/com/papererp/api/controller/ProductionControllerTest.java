package com.papererp.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papererp.api.filter.JwtAuthenticationFilter;
import com.papererp.api.security.JwtTokenService;
import com.papererp.common.domain.MassBalanceStatus;
import com.papererp.common.domain.RollStatus;
import com.papererp.common.domain.SlittingRunStatus;
import com.papererp.common.domain.WasteDisposition;
import com.papererp.core.dto.ProductionDto;
import com.papererp.core.service.JumboRollService;
import com.papererp.core.service.SlittingOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductionController Integration Tests")
class ProductionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JumboRollService jumboRollService;

    @MockBean
    private SlittingOperationService slittingOperationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("POST /api/v1/production/slitting-operations - Should return 200 with balanced mass result")
    void shouldExecuteSlittingOperation() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID machineId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID plantId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        ProductionDto.SlittingRunDto runDto = new ProductionDto.SlittingRunDto(
                runId, tenantId, plantId, parentId, machineId, operatorId, "IDEMP-TEST-001",
                new BigDecimal("3050.00"), new BigDecimal("50.00"), new BigDecimal("3000.00"),
                new BigDecimal("2800.00"), new BigDecimal("2925.00"), new BigDecimal("65.00"),
                new BigDecimal("-10.00"), MassBalanceStatus.BALANCED_WITHIN_TOLERANCE,
                WasteDisposition.BALED_FOR_RECYCLE, SlittingRunStatus.COMPLETED,
                Instant.now(), Instant.now(), Collections.emptyList()
        );

        when(slittingOperationService.executeSlitting(any())).thenReturn(runDto);

        var cuts = List.of(
                new ProductionDto.SlittingCutDto(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null),
                new ProductionDto.SlittingCutDto(2, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("1255.00"), new BigDecimal("10.00"), null),
                new ProductionDto.SlittingCutDto(3, new BigDecimal("700.00"), new BigDecimal("1000.00"), new BigDecimal("442.00"), new BigDecimal("7.00"), null)
        );

        ProductionDto.ExecuteSlittingRequest request = new ProductionDto.ExecuteSlittingRequest(
                parentId, machineId, "IDEMP-TEST-001",
                new BigDecimal("65.00"),
                WasteDisposition.BALED_FOR_RECYCLE,
                cuts
        );

        mockMvc.perform(post("/api/v1/production/slitting-operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(runId.toString()))
                .andExpect(jsonPath("$.data.scaleVarianceKg").value(-10.00))
                .andExpect(jsonPath("$.data.massBalanceStatus").value("BALANCED_WITHIN_TOLERANCE"));
    }
}
