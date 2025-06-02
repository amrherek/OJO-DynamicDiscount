package com.atos.dynamicdiscount.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atos.dynamicdiscount.processor.manager.AsyncExecutor;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    @Autowired
    private AsyncExecutor asyncExecutor;

    /**
     * Endpoint to process discounts.
     *
     * @param mode The mode (e.g., "c" for new request, "r" for resume).
     * @param inputValue The input value (e.g., bill cycle or request ID).
     * @return Response indicating success or failure.
     */
    
    @PostMapping("/process")
    public ResponseEntity<String> processDiscounts(
            @RequestParam String mode,
            @RequestParam String inputValue) {
    	asyncExecutor.processDiscountsAsync(mode, inputValue);
        return ResponseEntity.ok("Processing initiated. You can check the logs for updates.");
    }    
}
