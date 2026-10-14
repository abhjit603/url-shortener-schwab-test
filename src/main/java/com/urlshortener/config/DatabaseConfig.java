package com.urlshortener.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    DataSource dataSource(AppProperties props) throws IOException {
        Path dir = Path.of(props.dataPath()).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path dbFile = dir.resolve("urlshortener");
        String url = "jdbc:h2:file:" + dbFile + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";

        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url(url)
                .build();
    }
}
