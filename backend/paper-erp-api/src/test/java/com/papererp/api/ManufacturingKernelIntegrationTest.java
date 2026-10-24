package com.papererp.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ManufacturingKernelIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID tenantAId;
    private UUID tenantBId;
    private UUID plantAId;
    private UUID plantBId;
    private UUID gradeFlutingId;
    private UUID pm1MachineId;
    private UUID slitter1MachineId;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        // Clean test data
        jdbcTemplate.execute("DELETE FROM inventory_movements");
        jdbcTemplate.execute("DELETE FROM quality_inspections");
        jdbcTemplate.execute("DELETE FROM slit_rolls");
        jdbcTemplate.execute("DELETE FROM slitting_runs");
        jdbcTemplate.execute("DELETE FROM jumbo_rolls");
        jdbcTemplate.execute("DELETE FROM machines");
        jdbcTemplate.execute("DELETE FROM paper_grades");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM plants");
        jdbcTemplate.execute("DELETE FROM tenants");

        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();

        // 1. Create 2 isolated tenants
        jdbcTemplate.update("INSERT INTO tenants (id, code, name_ar, name_en) VALUES (?, 'TENANT_A', 'مصنع الورق الحديث', 'Modern Paper Mill')", tenantAId);
        jdbcTemplate.update("INSERT INTO tenants (id, code, name_ar, name_en) VALUES (?, 'TENANT_B', 'مصنع النيل للكرتون', 'Nile Board Mill')", tenantBId);

        // 2. Create Plants
        plantAId = UUID.randomUUID();
        plantBId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO plants (id, tenant_id, code, name_ar, name_en) VALUES (?, ?, 'PLANT_A1', 'مجمع السادات', 'Sadat Complex')", plantAId, tenantAId);
        jdbcTemplate.update("INSERT INTO plants (id, tenant_id, code, name_ar, name_en) VALUES (?, ?, 'PLANT_B1', 'مجمع العاشر', '10th Ramadan Complex')", plantBId, tenantBId);

        // 3. Create User Operator for Tenant A
        operatorId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, tenant_id, plant_id, username, password_hash, full_name_ar, role) VALUES (?, ?, ?, 'operator_a', 'hash', 'أحمد محمود', 'PRODUCTION_OPERATOR')",
                operatorId, tenantAId, plantAId);

        // 4. Create Paper Grade for Tenant A
        gradeFlutingId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO paper_grades (id, tenant_id, code, name_ar, name_en, type, default_gsm_min, default_gsm_max) VALUES (?, ?, 'FLUTE_125', 'فلوتنج 125', 'Fluting 125', 'FLUTING', 120, 130)",
                gradeFlutingId, tenantAId);

        // 5. Create Machines for Tenant A
        pm1MachineId = UUID.randomUUID();
        slitter1MachineId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, name_en, type, max_trim_width_mm) VALUES (?, ?, ?, 'PM1', 'ماكينة ورق 1', 'Paper Machine 1', 'PAPER_MACHINE', 2500)",
                pm1MachineId, tenantAId, plantAId);
        jdbcTemplate.update("INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, name_en, type, max_trim_width_mm) VALUES (?, ?, ?, 'SLIT1', 'مقص رولات 1', 'Slitter Rewinder 1', 'SLITTER_REWINDER', 2500)",
                slitter1MachineId, tenantAId, plantAId);
    }

    @Test
    @DisplayName("Milestone 1.1: Composite FK prevents cross-tenant relational linkage")
    void testCrossTenantIsolationEnforcedByCompositeFk() {
        UUID crossTenantMachineId = UUID.randomUUID();

        // Attempting to create a machine for Tenant B that references Plant A (belonging to Tenant A)
        // Must fail with a composite foreign key violation
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "INSERT INTO machines (id, tenant_id, plant_id, code, name_ar, name_en, type, max_trim_width_mm) " +
                    "VALUES (?, ?, ?, 'HACK_PM', 'ماكينة غير مصرح بها', 'Unauthorized Machine', 'PAPER_MACHINE', 2400)",
                    crossTenantMachineId, tenantBId, plantAId // tenant B referencing plant A!
            );
        }).as("Expected composite FK violation when referencing another tenant's plant").isNotNull();
    }

    @Test
    @DisplayName("Milestone 1.2: Atomic Slitting Operation with Mass Balance & Dual-Entry Ledger")
    void testAtomicSlittingExecutionAndMassBalance() {
        UUID jumboId = UUID.randomUUID();
        String jumboSerial = "JMB-2026-001";
        double deckleWidth = 2400.0;
        double jumboNetWeight = 3000.0;
        double jumboGrossWeight = 3020.0;

        // Step 1: Record Jumbo Roll production
        transactionTemplate.execute(status -> {
            jdbcTemplate.update(
                    "INSERT INTO jumbo_rolls (id, tenant_id, plant_id, machine_id, paper_grade_id, serial_number, target_gsm, deckle_width_mm, gross_weight_kg, net_weight_kg, status, manufactured_at, operator_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 125, ?, ?, ?, 'ACTIVE_WIP', CURRENT_TIMESTAMP, ?)",
                    jumboId, tenantAId, plantAId, pm1MachineId, gradeFlutingId, jumboSerial, deckleWidth, jumboGrossWeight, jumboNetWeight, operatorId
            );

            // Record Jumbo WIP receipt in inventory ledger
            jdbcTemplate.update(
                    "INSERT INTO inventory_movements (tenant_id, jumbo_roll_id, movement_type, net_weight_kg, actor_id, reason) " +
                    "VALUES (?, ?, 'PRODUCTION_RECEIPT', ?, ?, 'Pope Reel Output to WIP')",
                    tenantAId, jumboId, jumboNetWeight, operatorId
            );
            return null;
        });

        // Step 2: Execute Atomic Slitting Run
        // Cut pattern: 2x 1000mm (1245 kg each) + 1x 350mm (435 kg) + 50mm trim waste (65 kg) + 10 kg variance = 3000 kg
        UUID slittingRunId = UUID.randomUUID();
        double trimWasteKg = 65.0;
        double varianceKg = 10.0;
        double totalChildWeight = 1245.0 + 1245.0 + 435.0; // 2925.0 kg

        transactionTemplate.execute(status -> {
            // A. Update parent jumbo to FULLY_SLIT
            jdbcTemplate.update("UPDATE jumbo_rolls SET status = 'FULLY_SLIT', version = version + 1 WHERE id = ? AND tenant_id = ?", jumboId, tenantAId);

            // B. Create Slitting Run
            jdbcTemplate.update(
                    "INSERT INTO slitting_runs (id, tenant_id, parent_jumbo_id, machine_id, operator_id, input_jumbo_weight_kg, total_child_weight_kg, trim_waste_kg, scale_variance_kg, started_at, completed_at, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'COMPLETED')",
                    slittingRunId, tenantAId, jumboId, slitter1MachineId, operatorId, jumboNetWeight, totalChildWeight, trimWasteKg, varianceKg
            );

            // C. Insert Jumbo CONSUMPTION movement
            jdbcTemplate.update(
                    "INSERT INTO inventory_movements (tenant_id, jumbo_roll_id, movement_type, net_weight_kg, actor_id, reason) " +
                    "VALUES (?, ?, 'STATUS_CHANGE', ?, ?, 'Jumbo consumed in slitting run')",
                    tenantAId, jumboId, jumboNetWeight, operatorId
            );

            // D. Generate 3 Child Rolls & PRODUCTION_RECEIPT movements
            double[][] cuts = {
                    {1000.0, 1245.0, 1.0},
                    {1000.0, 1245.0, 2.0},
                    {350.0, 435.0, 3.0}
            };

            for (double[] cut : cuts) {
                UUID childRollId = UUID.randomUUID();
                String childSerial = "SLT-" + (int)cut[2] + "-" + jumboSerial;
                String barcode = "BC-" + childSerial;
                double width = cut[0];
                double net = cut[1];
                double gross = net + 2.5; // tare
                int seq = (int)cut[2];

                jdbcTemplate.update(
                        "INSERT INTO slit_rolls (id, tenant_id, parent_jumbo_id, slitting_run_id, paper_grade_id, serial_number, barcode, cut_sequence, width_mm, measured_gsm, gross_weight_kg, tare_weight_kg, net_weight_kg, quality_status, inventory_status, slitted_at, operator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 125, ?, 2.5, ?, 'PENDING_INSPECTION', 'IN_STOCK', CURRENT_TIMESTAMP, ?)",
                        childRollId, tenantAId, jumboId, slittingRunId, gradeFlutingId, childSerial, barcode, seq, width, gross, net, operatorId
                );

                jdbcTemplate.update(
                        "INSERT INTO inventory_movements (tenant_id, slit_roll_id, movement_type, net_weight_kg, actor_id, reason) " +
                        "VALUES (?, ?, 'PRODUCTION_RECEIPT', ?, ?, 'Slit Roll Produced from Jumbo')",
                        tenantAId, childRollId, net, operatorId
                );
            }

            return null;
        });

        // Step 3: Verify Mass Balance and Invariants
        // Verify Jumbo status
        String finalJumboStatus = jdbcTemplate.queryForObject("SELECT status FROM jumbo_rolls WHERE id = ?", String.class, jumboId);
        assertThat(finalJumboStatus).isEqualTo("FULLY_SLIT");

        // Verify child roll count & total weight
        Integer childCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM slit_rolls WHERE parent_jumbo_id = ?", Integer.class, jumboId);
        assertThat(childCount).isEqualTo(3);

        Double actualChildSum = jdbcTemplate.queryForObject("SELECT SUM(net_weight_kg) FROM slit_rolls WHERE parent_jumbo_id = ?", Double.class, jumboId);
        assertThat(actualChildSum).isEqualTo(totalChildWeight);

        // Verify Mass Balance: Child Sum + Trim Waste + Variance == Jumbo Net Weight
        assertThat(actualChildSum + trimWasteKg + varianceKg).isEqualTo(jumboNetWeight);

        // Verify Inventory Movements: Exactly 1 Jumbo Receipt + 1 Jumbo Consumption + 3 Child Receipts = 5 movements
        Integer totalMovements = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE tenant_id = ?", Integer.class, tenantAId);
        assertThat(totalMovements).isEqualTo(5);
    }

    @Test
    @DisplayName("Milestone 1.3: Atomic Rollback on Slitting Failure leaves zero partial state")
    void testAtomicRollbackOnFailureLeavesZeroPartialState() {
        UUID jumboId = UUID.randomUUID();
        double deckleWidth = 2400.0;

        // Record Jumbo
        jdbcTemplate.update(
                "INSERT INTO jumbo_rolls (id, tenant_id, plant_id, machine_id, paper_grade_id, serial_number, target_gsm, deckle_width_mm, gross_weight_kg, net_weight_kg, status, manufactured_at, operator_id) " +
                "VALUES (?, ?, ?, ?, ?, 'JMB-FAIL-TEST', 125, ?, 3020, 3000, 'ACTIVE_WIP', CURRENT_TIMESTAMP, ?)",
                jumboId, tenantAId, plantAId, pm1MachineId, gradeFlutingId, deckleWidth, operatorId
        );

        // Attempt invalid slitting operation that throws an exception halfway through
        try {
            transactionTemplate.execute(status -> {
                // Change status
                jdbcTemplate.update("UPDATE jumbo_rolls SET status = 'FULLY_SLIT' WHERE id = ?", jumboId);

                // Insert 1 child roll
                jdbcTemplate.update(
                        "INSERT INTO slit_rolls (id, tenant_id, parent_jumbo_id, paper_grade_id, serial_number, barcode, cut_sequence, width_mm, measured_gsm, gross_weight_kg, net_weight_kg, slitted_at, operator_id) " +
                        "VALUES (?, ?, ?, ?, 'SLT-TEMP-01', 'BC-TEMP', 1, 1000, 125, 1247.5, 1245.0, CURRENT_TIMESTAMP, ?)",
                        UUID.randomUUID(), tenantAId, jumboId, gradeFlutingId, operatorId
                );

                // Simulate validation failure (cuts exceed deckle width)
                throw new RuntimeException("Simulated Validation Error: Width sum exceeds 2400mm deckle!");
            });
        } catch (Exception ignored) {
            // Expected rollback
        }

        // Verify zero partial changes survived
        String jumboStatusAfterRollback = jdbcTemplate.queryForObject("SELECT status FROM jumbo_rolls WHERE id = ?", String.class, jumboId);
        assertThat(jumboStatusAfterRollback).isEqualTo("ACTIVE_WIP"); // reverted to ACTIVE_WIP

        Integer childRollsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM slit_rolls WHERE parent_jumbo_id = ?", Integer.class, jumboId);
        assertThat(childRollsCount).isEqualTo(0); // 0 orphaned child rolls

        Integer movementsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE tenant_id = ?", Integer.class, tenantAId);
        assertThat(movementsCount).isEqualTo(0); // 0 orphaned movements
    }
}
