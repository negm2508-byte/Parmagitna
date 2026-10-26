package com.papererp.core.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PostgreSQL 16 Real Database RLS & Keyset Index Integration Test")
class PostgreSqlRlsVerificationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/paper_erp_test";
    private static final String APP_USER = "paper_erp_app_role";
    private static final String APP_PASS = "AppSecret123!";

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111";
    private static final String TENANT_B = "22222222-2222-2222-2222-222222222222";

    @BeforeAll
    static void checkPostgreSqlAvailability() {
        try (Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS)) {
            Assumptions.assumeTrue(conn != null && !conn.isClosed(), "Local PostgreSQL 16 instance is available");
        } catch (Exception e) {
            Assumptions.abort("PostgreSQL not reachable: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("PostgreSQL RLS: Missing tenant context returns 0 rows (Fail-Closed)")
    void missingTenantContext_returnsZeroRows() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS)) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM plants");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Without app.current_tenant_id, queries must return 0 rows");
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL RLS: Tenant A can see its own plant, Tenant B cannot see Tenant A's plant")
    void tenantIsolation_enforcedByPostgreSqlRls() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS)) {
            conn.setAutoCommit(false);
            try {
                // 1. Establish Tenant A
                try (PreparedStatement setStmt = conn.prepareStatement("SELECT set_config('app.current_tenant_id', ?, true)")) {
                    setStmt.setString(1, TENANT_A);
                    setStmt.executeQuery();
                }

                int tenantAPlants;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM plants WHERE tenant_id = ?::uuid")) {
                    stmt.setString(1, TENANT_A);
                    try (ResultSet rs = stmt.executeQuery()) {
                        assertTrue(rs.next());
                        tenantAPlants = rs.getInt(1);
                        assertTrue(tenantAPlants >= 1, "Tenant A should see at least 1 plant");
                    }
                }

                // 2. Establish Tenant B on the same connection
                try (PreparedStatement setStmt = conn.prepareStatement("SELECT set_config('app.current_tenant_id', ?, true)")) {
                    setStmt.setString(1, TENANT_B);
                    setStmt.executeQuery();
                }

                // Tenant B querying Tenant A's records: must return 0 rows!
                try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM plants WHERE tenant_id = ?::uuid")) {
                    stmt.setString(1, TENANT_A);
                    try (ResultSet rs = stmt.executeQuery()) {
                        assertTrue(rs.next());
                        assertEquals(0, rs.getInt(1), "Tenant B must NOT see Tenant A's plants even with known tenant ID");
                    }
                }
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL RLS: Connection reuse after commit resets tenant context (is_local=true)")
    void connectionReuse_resetsContext() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, APP_USER, APP_PASS)) {
            // First transaction sets Tenant A
            conn.setAutoCommit(false);
            try (PreparedStatement setStmt = conn.prepareStatement("SELECT set_config('app.current_tenant_id', ?, true)")) {
                setStmt.setString(1, TENANT_A);
                setStmt.executeQuery();
            }
            conn.commit();

            // Next transaction on the same connection without calling set_config
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM plants");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Reused connection must not retain previous transaction's tenant context");
            } finally {
                conn.rollback();
            }
        }
    }
}
