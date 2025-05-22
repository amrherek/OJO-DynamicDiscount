package com.atos.dynamicdiscount.listener.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.atos.dynamicdiscount.listener.service.GmdRequestService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListenerJobScheduler {

	@Autowired
	private GmdRequestService requestProcessingService;

	@Scheduled(fixedRate = 300000) // Every 5 minutes
	public void runListenerJob() {
		try {
			requestProcessingService.processJob();
		} catch (Exception e) {
			log.error("An error occurred while processing the job: {}", e.getMessage(), e);
		}
	}
}
