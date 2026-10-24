package com.papererp.api;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Flyway V1 Initial Schema Migration Applied Successfully")
    void verifyFlywayMigrationApplied() {
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertThat(appliedMigrations).isNotEmpty();
        assertThat(appliedMigrations[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(appliedMigrations[0].getDescription()).isEqualTo("initial schema");
        assertThat(appliedMigrations[0].getState().isApplied()).isTrue();
    }

    @Test
    @DisplayName("Verify All Core Manufacturing Tables Exist in Clean Database")
    void verifyRequiredTablesExist() throws Exception {
        Set<String> expectedTables = Set.of(
                "tenants", "plants", "users", "paper_grades", "machines",
                "warehouses", "warehouse_locations", "jumbo_rolls",
                "slit_rolls", "quality_inspections", "inventory_movements", "audit_events"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        for (String expectedTable : expectedTables) {
            assertThat(actualTables)
                    .as("Expected table '%s' to exist in database schema", expectedTable)
                    .contains(expectedTable);
        }
    }
}
