package com.papererp.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaperErpApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Spring Application Context Loads Successfully")
    void contextLoads() {
        // Verifies all Spring components, datasource, JPA, and Flyway start with 0 errors
    }

    @Test
    @DisplayName("Smoke Test: GET /api/v1/health returns 200 OK with UP status")
    void testHealthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.system", is("PaperERP Enterprise")))
                .andExpect(jsonPath("$.databaseConnected", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
