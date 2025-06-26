package com.atos.dynamicdiscount.processor.service.cp;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConnectionPoolScalingService {

    private final HikariDataSource dataSource;
    private final ThreadPoolTaskExecutor taskExecutor;

    // HikariCP default and scaled parameters
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int defaultMinSize;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int defaultMaxSize;

    @Value("${hikari.scaled-min-size:40}")
    private int scaledMinSize;

    @Value("${hikari.scaled-max-size:70}")
    private int scaledMaxSize;

    // Thread pool default and scaled parameters
    @Value("${threadpool.default-core-size:5}")
    private int defaultCorePoolSize;

    @Value("${threadpool.default-max-size:10}")
    private int defaultMaxPoolSize;

    @Value("${threadpool.scaled-core-size:20}")
    private int scaledCorePoolSize;

    @Value("${threadpool.scaled-max-size:50}")
    private int scaledMaxPoolSize;

    @Value("${threadpool.default-queue-capacity:100}")
    private int defaultQueueCapacity;

    @Value("${threadpool.scaled-queue-capacity:500}")
    private int scaledQueueCapacity;

    public ConnectionPoolScalingService(HikariDataSource dataSource, ThreadPoolTaskExecutor taskExecutor) {
        this.dataSource = dataSource;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Scale up both the Hikari connection pool and the thread pool.
     */
    public void scaleUp() {
        scaleHikariPool(scaledMinSize, scaledMaxSize);
        scaleThreadPool(scaledCorePoolSize, scaledMaxPoolSize, scaledQueueCapacity);
    }

    
     //Scale down both the Hikari connection pool and the thread pool.
    public void scaleDown() {
        scaleHikariPool(defaultMinSize, defaultMaxSize);
        scaleThreadPool(defaultCorePoolSize, defaultMaxPoolSize, defaultQueueCapacity);
    }

    
     //Scale the HikariCP connection pool.
    private void scaleHikariPool(int minSize, int maxSize) {
        dataSource.setMinimumIdle(minSize);
        dataSource.setMaximumPoolSize(maxSize);
        log.info("HikariCP pool scaled: min={}, max={}", minSize, maxSize);
    }

    
     // Scale the thread pool.
    private void scaleThreadPool(int corePoolSize, int maxPoolSize, int queueCapacity) {
        taskExecutor.setCorePoolSize(corePoolSize);
        taskExecutor.setMaxPoolSize(maxPoolSize);
        taskExecutor.setQueueCapacity(queueCapacity);
        log.info("Thread pool scaled: core={}, max={}, queue={}", corePoolSize, maxPoolSize, queueCapacity);
    }
}
