package com.atos.dynamicdiscount.processor.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

    // Database connection properties
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

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout; // Maximum idle time before removing a connection (in ms)

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime; // Maximum lifetime of a connection in the pool (in ms)

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout; // Maximum time to wait for a connection (in ms)

    @Value("${spring.datasource.hikari.socket-timeout:30000}")
    private long socketTimeout; // TCP-level timeout (in ms)

    @Value("${spring.datasource.hikari.validation-timeout:5000}")
    private long validationTimeout; // Validation timeout for connections (in ms)

    @Value("${spring.datasource.hikari.keepalive-time:30000}")
    private long keepAliveTime; // Interval for keep-alive tests (in ms)

    @Bean
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        
        // Basic database connection setup
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUsername);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setDriverClassName(driverClassName);

        // HikariCP pool settings
        hikariConfig.setMinimumIdle(minimumIdle);
        hikariConfig.setMaximumPoolSize(maximumPoolSize);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setPoolName("DynamicDiscountHikariPool");
        hikariConfig.setAutoCommit(false);

        // Connection validation setup (Oracle optimized)
        hikariConfig.addDataSourceProperty("oracle.jdbc.fastConnectionValidation", "true");
        hikariConfig.setValidationTimeout(validationTimeout);
        hikariConfig.setKeepaliveTime(keepAliveTime); 


        // TCP-level protection
        hikariConfig.addDataSourceProperty("socketTimeout", socketTimeout);

        // Optimizations for Oracle Database
        hikariConfig.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "100");
        
        return new HikariDataSource(hikariConfig);
    }
}



