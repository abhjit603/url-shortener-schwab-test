package com.urlshortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ActuatorProbeIntegrationTest.ProbeConfig.class)
class ActuatorProbeIntegrationTest {

    private static final String TEST_DATA_PATH = Path.of(
                    System.getProperty("java.io.tmpdir"), "url-shortener-probes-" + UUID.randomUUID())
            .toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableDatabaseHealthIndicator dbHealthIndicator;

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("app.data-path", () -> TEST_DATA_PATH);
    }

    @BeforeEach
    void restoreDatabaseHealth() {
        dbHealthIndicator.setAvailable(true);
    }

    @Test
    void exposesActuatorHealthGroupsAndMainPortProbePaths() throws Exception {
        assertUp("/actuator/health");
        assertUp("/actuator/health/liveness");
        assertUp("/actuator/health/readiness");
        assertUp("/livez");
        assertUp("/readyz");
    }

    @Test
    void databaseFailureMakesReadinessDownButKeepsLivenessUp() throws Exception {
        dbHealthIndicator.setAvailable(false);

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
        mockMvc.perform(get("/readyz"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
        assertUp("/actuator/health/liveness");
        assertUp("/livez");
    }

    private void assertUp(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfig {

        @Bean(name = "dbHealthIndicator")
        SwitchableDatabaseHealthIndicator dbHealthIndicator() {
            return new SwitchableDatabaseHealthIndicator();
        }
    }

    static class SwitchableDatabaseHealthIndicator implements HealthIndicator {

        private volatile boolean available = true;

        @Override
        public Health health() {
            return available ? Health.up().build() : Health.down().build();
        }

        void setAvailable(boolean available) {
            this.available = available;
        }
    }
}
