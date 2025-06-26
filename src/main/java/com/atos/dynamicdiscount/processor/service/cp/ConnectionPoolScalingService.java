package com.atos.dynamicdiscount.processor.service.cp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
public class ConnectionPoolScalingService {

    private final HikariDataSource dataSource;

    // HikariCP default and scaled parameters
    @Value("${spring.datasource.hikari.minimum-idle}")
    private int defaultMinSize;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int defaultMaxSize;

    @Value("${hikari.scaled-min-size}")
    private int scaledMinSize;

    @Value("${hikari.scaled-max-size}")
    private int scaledMaxSize;



    public void scaleUp() {
        scaleHikariPool(scaledMinSize, scaledMaxSize);
    }

    public void scaleDown() {
        scaleHikariPool(defaultMinSize, defaultMaxSize);
    }

    private void scaleHikariPool(int minSize, int maxSize) {
        dataSource.setMinimumIdle(minSize);
        dataSource.setMaximumPoolSize(maxSize);
        log.info("HikariCP pool scaled: min={}, max={}", minSize, maxSize);
    }
}
