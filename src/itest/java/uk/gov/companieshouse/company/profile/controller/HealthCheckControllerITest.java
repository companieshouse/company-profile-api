package uk.gov.companieshouse.company.profile.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class HealthCheckControllerITest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Successfully returns health status")
    void returnHealthStatusSuccessfully() throws Exception {
        mockMvc.perform(get("/healthcheck")).andExpect(status().isOk());
    }
}
