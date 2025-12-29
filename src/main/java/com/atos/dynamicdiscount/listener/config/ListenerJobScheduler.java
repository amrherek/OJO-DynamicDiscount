package com.atos.dynamicdiscount.listener.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.atos.dynamicdiscount.listener.service.FirstLoginTrackService;
import com.atos.dynamicdiscount.listener.service.GmdRequestService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ListenerJobScheduler {
	
    @Value("${job.scheduler.fixedRate:3000000}") // Default to 5 minutes
    private long fixedRate;

	@Autowired
	private GmdRequestService requestProcessingService;
	
    @Autowired
    private FirstLoginTrackService firstLoginTrackService;
    
    @Value("${firstlogin.occ.enabled:true}")
    private boolean firstLoginOccEnabled;

    



    @Scheduled(fixedRateString = "#{T(java.lang.Long).parseLong('${job.scheduler.fixedRate:3000000}')}")
	public void runListenerJob() {
		try {
			requestProcessingService.processJob();
		} catch (Exception e) {
			log.error("An error occurred while processing the job: {}", e.getMessage(), e);
		}
	}
    
    
    
    // New scheduled job for first login delta
    @Scheduled(fixedRateString = "#{T(java.lang.Long).parseLong('${firstlogin.occ.interval:1800000}')}")
    public void runFirstLoginDeltaJob() {
    	
        if (!firstLoginOccEnabled) {
            log.debug("First login delta scheduler is disabled.");
            return;
        }
    	
        try {
            firstLoginTrackService.processJob();
        } catch (Exception e) {
            log.error("Error while processing first login delta: {}", e.getMessage(), e);
        }
    }
    
}
