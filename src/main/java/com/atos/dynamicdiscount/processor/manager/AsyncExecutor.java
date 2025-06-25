package com.atos.dynamicdiscount.processor.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.processor.service.cp.ConnectionPoolScalingService;

@Service
public class AsyncExecutor {

    @Autowired
    private ExecutionManager executionManager;
    
    @Autowired
    private  ConnectionPoolScalingService poolScalingService;

    @Async
    public void processDiscountsAsync(String mode, String inputValue) {
    	
        try {
            // Scale up the pool before processing
            poolScalingService.scaleUp(); 

            // Process the discounts
            executionManager.processDiscounts(mode, inputValue);

        } finally {
            // Scale down the pool after processing
            poolScalingService.scaleDown();
        }
    }
}
