package com.atos.dynamicdiscount.processor.manager;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.enums.BillCycle;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;
import com.atos.dynamicdiscount.processor.service.billcycle.BillCycleService;
import com.atos.dynamicdiscount.processor.service.request.DiscountRequestService;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class ExecutionManager {

    @Autowired
    private BillCycleService billCycleService;

    @Autowired
    private DiscountRequestService requestService;

    @Autowired
    private BatchProcessor batchProcessor;
    
    @Autowired
    private  DynDiscConfigurations configurations;


 
    private static final String NEW_REQUEST = "c"; // Mode for creating a new request
    private static final String RESUME_REQUEST = "r"; // Mode for resuming an existing request
    

    /**
     * Processes discounts based on the given mode and input.
     */
    public void processDiscounts(String mode, String inputValue) {
        log.info("----------------------------------------------------------");
        log.info("Starting discount processing with mode: {}, input: {}", mode, inputValue);

        try {
            if (NEW_REQUEST.equals(mode)) {
                processNewRequest(inputValue);
            } else if (RESUME_REQUEST.equals(mode)) {
                processResumeRequest(inputValue);
            } else {
                throw new IllegalArgumentException("Invalid mode: " + mode);
            }
        } catch (Exception e) {
            log.error("X Error processing discounts for mode '{}' and input '{}': {}", mode, inputValue, e.getMessage(), e);
        }
    }

    /**
     * Handles the creation of a new request.
     */
    private void processNewRequest(String billCycle) { 
        log.info("Processing new request for bill cycle: {}", billCycle);

        // Check for ongoing requests
        DynDiscRequest ongoingRequest = requestService.fetchOngoingRequest();
        if (ongoingRequest != null) {
            log.error("X Cannot start a new request: Request {} is already in progress.", ongoingRequest.getRequestId());
            return;
        }

        // Validate bill cycle and fetch cutoff date
        BillCycle billcycle = billCycleService.validateBillCycle(billCycle);
        if (billcycle == null) {
            return;
        }
        
        
        
        LocalDateTime cutoff = billCycleService.fetchCutoffDate(billCycle);
        if (cutoff == null) {
            return;
        }

        // refresh configurations
        configurations.refreshConfigurations();

        
        // Register and process the new request
        DynDiscRequest newRequest = requestService.registerNewRequest(billCycle, cutoff);
        if (newRequest == null) {
            log.warn("! No new request generated for bill cycle: {}", billCycle);
            return;
        }

        log.info("√ Processing started for new request ID: {}.", newRequest.getRequestId());

        processRequest(newRequest);
    }


    /**
     * Handles resuming an existing request.
     */
    private void processResumeRequest(String requestNumber) {
        log.info("Starting process to resume request with ID: {}", requestNumber);

        int requestId;
        try {
            requestId = Integer.parseInt(requestNumber);
        } catch (NumberFormatException e) {
            log.error("X Failed to parse request number: {}", requestNumber);
            return;
            //throw new InvalidRequestNumberException("Invalid request number: " + requestNumber, e);
        }

        DynDiscRequest request = requestService.fetchRequestById(requestId);
        if (request == null) {
            log.error("X Unable to resume: No request found with ID: {}", requestId);
            return;
            //throw new InvalidRequestNumberException("No request found with ID: " + requestId);
        }

        switch (request.getStatus()) {
            case "W":
                log.warn("! Unable to resume: Request {} is already in progress.", requestId);
                return;

            case "P":
                log.warn("! Unable to resume: Request {} is already completed.", requestId);
                return;

            case "F":
            	log.info("Resuming request ID: {}, current status is 'F'", requestId);
            	int updated=requestService.resetFailedContractsAndPackages(requestId);
                if (updated >0) {
                    log.info("√ Processing resumed for request ID {}. Total contracts to process: {}", requestId, updated);
                    processRequest(request);
                    log.info("√ Successfully completed processing for request ID {}.", requestId);
                } else {
                    log.warn("! SKIPPING processing for request ID {}: No contracts found with status 'I'.", requestId);
                }
                break;

            default:
                log.error("X Unable to process: Unexpected request status '{}' for request ID: {}", request.getStatus(), requestId);
                return;
                //throw new IllegalStateException("Unexpected request status: " + request.getStatus());
        }
    }

    /**
     * Processes a request and its associated contracts in batches.
     */
    
    private void processRequest(DynDiscRequest request) {
   
        log.info("Processing request ID: {}.", request.getRequestId());
        // Process contracts in batches
        batchProcessor.processRequestPackages(request);
        // Finalize the request
        requestService.finalizeRequest(request.getRequestId());
        log.info("Completed discount processing for request ID: {}", request.getRequestId());
    }
}
