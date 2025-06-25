package com.atos.dynamicdiscount.processor.service.cp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Data
public class ConnectionPoolScalingService {

    private final HikariDataSource dataSource;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int defaultMinSize;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int defaultMaxSize;

    @Value("${hikari.scaled-min-size:40}")
    private int scaledMinSize;

    @Value("${hikari.scaled-max-size:70}")
    private int scaledMaxSize;

    public ConnectionPoolScalingService(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void scaleUp() {
        dataSource.setMinimumIdle(scaledMinSize);
        dataSource.setMaximumPoolSize(scaledMaxSize);
        log.info("HikariCP pool scaled up: min={}, max={}", scaledMinSize, scaledMaxSize);
    }

    public void scaleDown() {
        dataSource.setMinimumIdle(defaultMinSize);
        dataSource.setMaximumPoolSize(defaultMaxSize);
        log.info("HikariCP pool scaled down: min={}, max={}", defaultMinSize, defaultMaxSize);
    }
}
