package com.northcare.insurance.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Configuration
@Profile("local")
public class LocalDatabaseConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        log.info("Starting embedded PostgreSQL for insurance (port 5435)...");
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setPort(5435)
                .start();

        try (Connection conn = pg.getPostgresDatabase().getConnection();
             Statement stmt = conn.createStatement()) {
            try { stmt.execute("CREATE ROLE northcare SUPERUSER LOGIN PASSWORD 'changeme'"); }
            catch (Exception e) { log.debug("Role 'northcare' already exists"); }
            try { stmt.execute("CREATE DATABASE northcare_insurance OWNER northcare"); }
            catch (Exception e) { log.debug("Database 'northcare_insurance' already exists"); }
        } catch (Exception e) {
            log.warn("Embedded DB setup warning: {}", e.getMessage());
        }
        log.info("Embedded PostgreSQL ready on port 5435");
        return pg;
    }

    @Bean
    @DependsOn("embeddedPostgres")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5435/northcare_insurance");
        config.setUsername("northcare");
        config.setPassword("changeme");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setPoolName("NorthCareInsurancePool-Local");
        config.setConnectionTimeout(30_000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }
}
