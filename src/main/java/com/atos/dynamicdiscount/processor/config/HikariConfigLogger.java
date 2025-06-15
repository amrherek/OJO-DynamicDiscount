package com.atos.dynamicdiscount.processor.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

@Configuration
public class HikariConfigLogger {
    private static final Logger log = LoggerFactory.getLogger(HikariConfigLogger.class);

    @Autowired
    private HikariDataSource dataSource;

    @PostConstruct
    public void logPoolInfo() {
        log.info("HikariCP Config - Max Pool Size: {}, Min Idle: {}, Active Connections: {}, Idle Connections: {}, Total Connections: {}",
                dataSource.getMaximumPoolSize(),
                dataSource.getMinimumIdle(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections());
    }
}
