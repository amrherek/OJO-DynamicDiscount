package com.atos.dynamicdiscount.processor.manager;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.exceptions.OngoingtRequestException;
import com.atos.dynamicdiscount.model.dto.NewRequestResultDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.processor.service.billcycle.BillCycleService;
import com.atos.dynamicdiscount.processor.service.request.DiscountRequestService;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages the discount processing flow: 
 * 1. Validate bill cycle & fetch cutoff. 
 * 2. Check for ongoing request (status 'W') and handle accordingly. 
 * 3. Process contracts in batches and finalize.
 */
@Service
@Slf4j
public class ExecutionManager {

    @Autowired
    private BillCycleService billCycleService;

    @Autowired
    private DiscountRequestService requestService;

    @Autowired
    private BatchProcessor batchProcessor;

    @Value("${processing.batch.size:1000}")
    private int batchSize;

    public void processDiscounts(String billCycle) {
        log.info("Starting discount processing for bill cycle: {}", billCycle);

        try {
            // 1) Validate bill cycle and fetch cutoff date
            billCycleService.validateBillCycle(billCycle);
            LocalDateTime cutoff = billCycleService.fetchCutoffDate(billCycle);

            // 2) Fetch ongoing request and validate
            DynDiscRequest ongoingRequest = requestService.fetchOngoingRequest();
            NewRequestResultDTO result;

            if (ongoingRequest == null) {
                // No ongoing: create new request
                result = requestService.registerNewRequest(billCycle, cutoff);
                if (result == null) {
                    log.warn("No new request generated for bill cycle: {}", billCycle);
                    return;
                }
            } else {
                // Ongoing exists: verify matching cycle & cutoff
                if (!ongoingRequest.getBillcycle().equals(billCycle) || 
                    !ongoingRequest.getBillPeriodEndDate().isEqual(cutoff)) {
                    throw new OngoingtRequestException(String.format(
                            "Cannot proceed with bill cycle '%s': ongoing request %d exists with different cycle or cutoff.",
                            billCycle, ongoingRequest.getRequestId()));
                }
                // Resume: reset and fetch pending contracts
                requestService.resetFailedContracts(ongoingRequest.getRequestId());
                List<DynDiscContract> resumeContracts = requestService.getContractsByStatus(ongoingRequest.getRequestId(), "I");
                result = new NewRequestResultDTO(ongoingRequest, resumeContracts);
            }

            // 3) Process in batches
            DynDiscRequest request = result.getRequest();
            List<DynDiscContract> contracts = result.getDynDiscContracts();
            batchProcessor.processInBatches(request, contracts, batchSize);

            // 4) Finalize request
            requestService.finalizeRequest(request.getRequestId());
            log.info("Completed discount processing for bill cycle: {}", billCycle);

        } catch (Exception e) {
            log.error("Error processing discounts for '{}': {}", billCycle, e.getMessage(), e);
        }
    }
}
