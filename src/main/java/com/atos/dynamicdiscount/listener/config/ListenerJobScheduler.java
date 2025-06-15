package com.atos.dynamicdiscount.listener.config;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.atos.dynamicdiscount.listener.service.GmdRequestService;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListenerJobScheduler {
	
	@Autowired
	private DataSource dataSource;

	@Autowired
	private  ThreadPoolTaskExecutor executor;
	
    @Value("${job.scheduler.fixedRate:3000000}") // Default to 5 minutes
    private long fixedRate;

	@Autowired
	private GmdRequestService requestProcessingService;

    @Scheduled(fixedRateString = "#{T(java.lang.Long).parseLong('${job.scheduler.fixedRate:3000000}')}")
	public void runListenerJob() {
		try {
			requestProcessingService.processJob();
		} catch (Exception e) {
			log.error("An error occurred while processing the job: {}", e.getMessage(), e);
		}
	}
    
    
	@Scheduled(fixedRate = 30000) // Adjust interval as needed
	public void logSystemStatus() {
	    // Log thread pool status
	    log.info("Thread pool: Active={}, Queue Size={}, Completed Tasks={}",
	        executor.getActiveCount(),
	        executor.getThreadPoolExecutor().getQueue().size(),
	        executor.getThreadPoolExecutor().getCompletedTaskCount());

	    // Log memory usage
	    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	    MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
	    long usedMemory = heapUsage.getUsed();
	    long maxMemory = heapUsage.getMax();
	    long committedMemory = heapUsage.getCommitted();

	    log.info("Heap Memory Usage: Used={} MB, Committed={} MB, Max={} MB",
	        toMB(usedMemory), toMB(committedMemory), toMB(maxMemory));

	    // Log HikariCP connection pool statistics
		if (dataSource instanceof HikariDataSource hikariDataSource) {
			log.info(
					"HikariCP Stats: Active Connections={}, Idle Connections={}, Total Connections={}, Threads Awaiting Connection={}, Max Connections={}",
					hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
					hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
					hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
					hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
					hikariDataSource.getMaximumPoolSize());
		} else {
			log.warn("DataSource is not an instance of HikariDataSource. Skipping HikariCP stats logging.");
		}
	}

	private static long toMB(long bytes) {
	    return bytes / (1024 * 1024);
	}
}
