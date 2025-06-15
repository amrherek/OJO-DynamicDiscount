package com.atos.dynamicdiscount.processor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableAsync
@EnableTransactionManagement
public class ThreadPoolConfig {

	@Value("${threadpool.corePoolSize}")
	private int corePoolSize;

	@Value("${threadpool.maxPoolSize}")
	private int maxPoolSize;

	@Value("${threadpool.queueCapacity}")
	private int queueCapacity;

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		// executor.setCorePoolSize(Runtime.getRuntime().availableProcessors()); //
		executor.setCorePoolSize(corePoolSize); // Minimum number of threads to keep in the pool
		executor.setMaxPoolSize(maxPoolSize); // Maximum number of threads
		executor.setQueueCapacity(queueCapacity); // Capacity of the queue before new tasks get rejected
		executor.setThreadNamePrefix("AsyncExecutor-");
		executor.initialize();
		return executor;
	}
	
	
	
}
