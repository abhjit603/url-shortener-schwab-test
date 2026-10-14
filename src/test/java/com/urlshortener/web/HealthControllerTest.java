package com.urlshortener.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.urlshortener.error.GlobalExceptionHandler;
import com.urlshortener.service.StoreProbe;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({GlobalExceptionHandler.class, HealthControllerTest.DatabaseConfig.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createStoreTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS short_link");
        jdbcTemplate.execute("CREATE TABLE short_link (id INTEGER)");
    }

    @Test
    void returnsUpWhenStoreQueryWorks() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void returnsStoreUnavailableProblemWhenStoreQueryFails() throws Exception {
        jdbcTemplate.execute("DROP TABLE short_link");

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("STORE_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Data store is unavailable"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DatabaseConfig {

        @Bean
        EmbeddedDatabase healthTestDatabase() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        JdbcTemplate healthTestJdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        StoreProbe storeProbe(JdbcTemplate jdbcTemplate) {
            return new StoreProbe(jdbcTemplate);
        }
    }
}
