package com.atos.dynamicdiscount.processor.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncExecutor {

    @Autowired
    private ExecutionManager executionManager;

    @Async
    public void processDiscountsAsync(String mode, String inputValue) {
    	executionManager.processDiscounts(mode, inputValue);
    }
}
