package com.atos.dynamicdiscount.processor.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    // HikariCP pool configurations
    @Value("${spring.datasource.hikari.minimum-idle:20}")
    private int minimumIdle; // Minimum number of idle connections

    @Value("${spring.datasource.hikari.maximum-pool-size:100}")
    private int maximumPoolSize; // Maximum number of connections in the pool

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout; // Maximum idle time before removing a connection (ms)

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime; // Maximum lifetime of a connection in the pool (ms)

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout; // Maximum time to wait for a connection (ms)

    @Value("${spring.datasource.hikari.connection-test-query:SELECT 1 FROM DUAL}")
    private String connectionTestQuery; // Query to validate connections

    @Value("${spring.datasource.hikari.keepalive-time:30000}")
    private long keepAliveTime; // Interval for keep-alive tests (ms)

    

    
    @Bean
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl); // JDBC URL
        hikariConfig.setUsername(dbUsername); // Database username
        hikariConfig.setPassword(dbPassword); // Database password
        hikariConfig.setDriverClassName(driverClassName); // Driver class name

        // Configure HikariCP properties
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setConnectionTestQuery(connectionTestQuery);
        hikariConfig.addDataSourceProperty("keepaliveTime", keepAliveTime); // Keep-alive tests

        return new HikariDataSource(hikariConfig);
    }
}
