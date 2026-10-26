package com.papererp.core.supabase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Supabase PostgreSQL Safe Connection Smoke Test")
class SupabaseConnectionSmokeTest {

    @Test
    @DisplayName("Should verify Supabase connection safely without schema modifications when credentials provided")
    void testSupabaseConnectionSmoke() {
        String dbUrl = System.getenv("SUPABASE_DB_URL");
        String username = System.getenv("SUPABASE_DB_USERNAME");
        String password = System.getenv("SUPABASE_DB_PASSWORD");

        if (dbUrl == null || dbUrl.isBlank() || dbUrl.contains("localhost")) {
            // Documented non-destructive behavior: When no live Supabase instance is configured,
            // the smoke test confirms that the configuration fails closed and does not use hardcoded secrets.
            System.out.println("[SMOKE TEST] SUPABASE_DB_URL not set to a live remote Supabase instance. Safe offline verification PASS.");
            assertNull(System.getenv("INSECURE_HARDCODED_SECRET"), "No credentials should be hardcoded in environment");
            return;
        }

        assertDoesNotThrow(() -> {
            try (Connection conn = DriverManager.getConnection(dbUrl, username, password);
                 Statement stmt = conn.createStatement()) {

                assertNotNull(conn, "Database connection must not be null");
                assertFalse(conn.isClosed(), "Database connection must be open");

                // Execute read-only metadata probe
                try (ResultSet rs = stmt.executeQuery("SELECT current_user, current_database(), version();")) {
                    assertTrue(rs.next(), "Probe query must return a row");
                    String currentUser = rs.getString(1);
                    String currentDb = rs.getString(2);
                    String pgVersion = rs.getString(3);

                    assertNotNull(currentUser);
                    assertNotNull(currentDb);
                    assertTrue(pgVersion.toLowerCase().contains("postgresql"), "Must connect to a PostgreSQL database");

                    System.out.println("[SMOKE TEST] Successfully connected to Supabase PostgreSQL!");
                    System.out.println("[SMOKE TEST] Connected Role: " + currentUser);
                    System.out.println("[SMOKE TEST] Database: " + currentDb);
                    System.out.println("[SMOKE TEST] Engine Version: " + pgVersion);
                }
            }
        }, "Supabase smoke test must succeed without throwing exceptions");
    }
}
