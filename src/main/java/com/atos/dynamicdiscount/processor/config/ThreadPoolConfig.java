package com.atos.dynamicdiscount.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

	@Value("${threadpool.corePoolSize:2")
	private int corePoolSize;

	@Value("${threadpool.maxPoolSize:2}")
	private int maxPoolSize;

	@Value("${threadpool.queueCapacity:50}")
	private int queueCapacity;

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// executor.setCorePoolSize(Runtime.getRuntime().availableProcessors()); //
		// Default: Number of CPU cores --// Minimum number of threads to keep in the
		// pool
		// executor.setMaxPoolSize(20); // Maximum number of threads
		executor.setCorePoolSize(corePoolSize); // Minimum number of threads to keep in the pool
		executor.setMaxPoolSize(maxPoolSize); // Maximum number of threads
		executor.setQueueCapacity(queueCapacity); // Capacity of the queue before new tasks get rejected
		executor.setThreadNamePrefix("AsyncExecutor-");
		executor.initialize();
		return executor;
	}
}
