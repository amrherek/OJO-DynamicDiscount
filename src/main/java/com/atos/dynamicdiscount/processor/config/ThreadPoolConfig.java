package com.atos.dynamicdiscount.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableAsync
@EnableTransactionManagement
@EnableRetry

public class ThreadPoolConfig {

	@Value("${threadpool.default-core-size}")
	private int corePoolSize;

	@Value("${threadpool.default-max-size}")
	private int maxPoolSize;

	@Value("${threadpool.default-queue-capacity}")
	private int queueCapacity;

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize); // Minimum number of threads to keep in the pool
		executor.setMaxPoolSize(maxPoolSize); // Maximum number of threads
		executor.setQueueCapacity(queueCapacity); // Capacity of the queue before new tasks get rejected
		executor.setThreadNamePrefix("AsyncExecutor-");
		executor.initialize();
		return executor;
	}
	
}
