package com.atos.dynamicdiscount.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler component that periodically logs system status, including thread pool,
 * memory usage, and HikariCP connection pool statistics.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SystemStatusScheduler {

    private final DataSource dataSource;
    private final ThreadPoolTaskExecutor executor;

    @Value("${system.status.scheduler.enabled:true}")
    private boolean isSchedulerEnabled;

    /**
     * Logs system status at a fixed rate defined by the scheduler interval.
     */
    @Scheduled(fixedRateString = "${system.status.scheduler.interval:30000}")
    public void logSystemStatus() {
        if (!isSchedulerEnabled) {
            log.debug("System Status Scheduler is disabled.");
            return;
        }

        log.info("========== System Status ==========");
        logThreadPoolStatus();
        logJvmMemoryStatus();
        logCPUUtilization();
        logHikariCPStatus();
        log.info("===================================");
    }

    /**
     * Logs thread pool statistics.
     */
    private void logThreadPoolStatus() {
        if (executor != null) {
            log.info("Thread Pool: Active={}, Queue Size={}, Completed Tasks={}",
                executor.getActiveCount(),
                executor.getThreadPoolExecutor().getQueue().size(),
                executor.getThreadPoolExecutor().getCompletedTaskCount());
        } else {
            log.warn("Thread Pool: Stats unavailable");
        }
    }

    /**
     * Logs heap memory usage statistics.
     */
    
    private void logJvmMemoryStatus() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        // Heap memory (object allocation space)
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
       
        log.info("Heap Memory: Used={} MB, Committed={} MB, Max={} MB",
            toMB(heap.getUsed()),
            toMB(heap.getCommitted()),
            toMB(heap.getMax()));
        
        /*
        // Non-heap memory (metaspace, code cache, etc.)
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        
        
        log.info("Non-Heap Memory: Used={} MB, Committed={} MB, Max={} MB",
            toMB(nonHeap.getUsed()),
            toMB(nonHeap.getCommitted()),
            toMB(nonHeap.getMax()));
        */
    }

 
    
    /**
     * Logs CPU utilization statistics.
     */
    private void logCPUUtilization() {
        com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double processLoad = osBean.getProcessCpuLoad() * 100;
        double systemLoad = osBean.getSystemCpuLoad() * 100;

        log.info("CPU Utilization: Process Load={}%, System Load={}%",
            String.format("%.2f", processLoad),
            String.format("%.2f", systemLoad));
    }

    /**
     * Logs HikariCP connection pool statistics.
     */
    private void logHikariCPStatus() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            log.info("HikariCP: Active Connections={}, Idle Connections={}, Total Connections={}, "
                   + "Threads Awaiting Connection={}, Max Connections={}",
                hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                hikariDataSource.getMaximumPoolSize());
        } else {
            log.warn("HikariCP: Stats unavailable");
        }
    }

    /**
     * Converts bytes to megabytes for readability.
     * 
     * @param bytes the size in bytes
     * @return the size in megabytes
     */
    private long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
