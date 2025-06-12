package com.atos.dynamicdiscount.processor.manager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.repository.DynDiscAssignRepository;
import com.atos.dynamicdiscount.repository.DynDiscContractRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessor {

    private final DynDiscAssignRepository assignRepository;
    private final DynDiscContractRepository contractRepo;
    private final ContractProcessor contractProcessor;

    /**
     * Processes contracts in batches. Each batch fetches associated discounts and
     * offers, processes contracts concurrently, and moves to the next batch.
     */
    @Transactional
    public void processInBatches(DynDiscRequest request, int batchSize) {
    	    
    	    /* start commenting

    	    log.info("Starting batch processing for request ID: {}, cutoff date: {}", requestId, cutoff);

    	    // Step 1: Prepare the temporary table
    	    assignRepository.truncateTempTable(); // Clear any existing data in the temp table
    	    int inserted = assignRepository.persistAssignedDiscountsToTempTable(requestId, cutoff);

    	    if (inserted == 0) {
    	        log.warn("No discounts found for the contracts in scope for request ID: {}", requestId);
    	        return;
    	    }
    	    log.info("Extracted {} discounts into the temp table.", inserted);
    	    
    	    
    	    // Step 2: Load all discounts from the temp table into memory
    	    long startTime = System.currentTimeMillis();
    	    List<DynDiscAssignDTO> allDiscounts = assignRepository.fetchAllDiscountsFromTempTable(requestId);
    	    Map<Integer, List<DynDiscAssignDTO>> discountsByCoId = allDiscounts.stream()
    	            .collect(Collectors.groupingBy(dto -> dto.getCoId().intValue()));
    	    long endTime = System.currentTimeMillis();

    	    log.info("Loaded {} discounts into memory in {} ms", allDiscounts.size(), (endTime - startTime));
    	    
    	    end commenting */
    	
    	
    		Integer requestId = request.getRequestId();
    		LocalDateTime cutoff = request.getBillPeriodEndDate();

    
    	    long startTime = System.currentTimeMillis();
    	    List<DynDiscAssignDTO> allDiscounts = assignRepository.fetchAllAssignedDiscounts(requestId, cutoff);
    	    Map<Integer, List<DynDiscAssignDTO>> discountsByCoId = allDiscounts.stream()
    	            .collect(Collectors.groupingBy(dto -> dto.getCoId().intValue()));
    	    Long endTime = System.currentTimeMillis();
    	    log.info("Loaded {} discounts into memory in {} ms", allDiscounts.size(), (endTime - startTime));


    	    
    	    // Step 3: Fetch all unprocessed contracts at once
    	    startTime = System.currentTimeMillis();
    	    List<DynDiscContract> unprocessedContracts = contractRepo.fetchAllUnprocessedContracts(requestId);
    	    endTime = System.currentTimeMillis();
    	    log.info("Fetched {} unprocessed contracts for requestId: {} in {} ms", 
    	            unprocessedContracts.size(), requestId, (endTime - startTime));

    	    if (unprocessedContracts.isEmpty()) {
    	        log.info("No unprocessed contracts found for request ID: {}", requestId);
    	        return;
    	    }

    	    log.info("Fetched {} unprocessed contracts for request ID: {}", unprocessedContracts.size(), requestId);

    	    
    	    // Step 4: Process contracts in batches
    	    int totalContracts = unprocessedContracts.size();
    	    int batchCount = (int) Math.ceil((double) totalContracts / batchSize);

    	    for (int i = 0; i < batchCount; i++) {
    	        int start = i * batchSize;
    	        int end = Math.min(start + batchSize, totalContracts);

    	        List<DynDiscContract> batch = unprocessedContracts.subList(start, end);

    	        log.info("→ Processing batch {} of {} (contracts {} to {})", i + 1, batchCount, start + 1, end);

    	        // Process each contract in the batch concurrently
    	        List<CompletableFuture<Void>> futures = batch.stream()
    	                .map(contract -> contractProcessor.processContract(request, contract,
    	                        discountsByCoId.getOrDefault(contract.getCoId(), Collections.emptyList())))
    	                .collect(Collectors.toList());

    	        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    	        log.info("✓ Completed processing batch {} of {}", i + 1, batchCount);
    	    }

    	    assignRepository.truncateTempTable(); // Clear temp table after processing
    	    log.info("All batches successfully processed for request ID: {}", requestId);
    	    
    	    
    }
    
}
