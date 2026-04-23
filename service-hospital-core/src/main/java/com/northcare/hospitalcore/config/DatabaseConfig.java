package com.northcare.hospitalcore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!local")
public class DatabaseConfig {

    @Value("${northcare.aws.region:us-east-1}")
    private String awsRegion;

    @Value("${northcare.aws.db-secret-name:}")
    private String dbSecretName;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/northcare_hospital}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:northcare}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:changeme}")
    private String datasourcePassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:5}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        if (StringUtils.hasText(dbSecretName)) {
            log.info("Loading DB credentials from AWS Secrets Manager: {}", dbSecretName);
            DbCredentials creds = loadCredentialsFromSecretsManager(dbSecretName);
            config.setJdbcUrl(buildJdbcUrl(creds));
            config.setUsername(creds.username());
            config.setPassword(creds.password());
        } else {
            log.info("Using local datasource configuration (no AWS secret name configured)");
            config.setJdbcUrl(datasourceUrl);
            config.setUsername(datasourceUsername);
            config.setPassword(datasourcePassword);
        }

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setPoolName("NorthCareHikariPool");
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    private DbCredentials loadCredentialsFromSecretsManager(String secretName) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> secretMap = mapper.readValue(secretJson, Map.class);

            return new DbCredentials(
                    secretMap.getOrDefault("host", "localhost"),
                    Integer.parseInt(secretMap.getOrDefault("port", "5432")),
                    secretMap.getOrDefault("dbname", "northcare_hospital"),
                    secretMap.get("username"),
                    secretMap.get("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load DB credentials from Secrets Manager: " + secretName, e);
        }
    }

    private String buildJdbcUrl(DbCredentials creds) {
        return String.format("jdbc:postgresql://%s:%d/%s", creds.host(), creds.port(), creds.dbname());
    }

    private record DbCredentials(String host, int port, String dbname, String username, String password) {}
}
