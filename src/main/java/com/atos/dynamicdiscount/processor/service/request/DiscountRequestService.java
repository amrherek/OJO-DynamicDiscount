package com.atos.dynamicdiscount.processor.service.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.NewRequestResultDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.model.entity.DynDiscStatistic;
import com.atos.dynamicdiscount.repository.DynDiscContractRepository;
import com.atos.dynamicdiscount.repository.DynDiscRequestRepository;
import com.atos.dynamicdiscount.repository.DynDiscStatisticRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling discount requests and associated operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscountRequestService {

    private final DynDiscRequestRepository requestRepo;
    private final DynDiscContractRepository contractRepo;
    private final DynDiscStatisticRepository statisticRepo;
    

    @PersistenceContext
    private EntityManager entityManager;
    

    
       
    /** Registers a new discount request and processes eligible contracts. */
    @Transactional
    public DynDiscRequest registerNewRequest(String billCycle, LocalDateTime cutoffDate) {
        log.info("Persisting eligible contracts for bill cycle '{}' and cutoff date '{}'.", billCycle, cutoffDate);

        int insertedRows = persistEligibleContracts(billCycle, cutoffDate);
        if (insertedRows == 0) {
            log.warn("No eligible contracts were found for bill cycle '{}' and cutoff date '{}'.", billCycle, cutoffDate);
            return null;
        }
        log.info("Found {} eligible contracts for bill cycle '{}' and cutoff date '{}'.", insertedRows, billCycle, cutoffDate);

        DynDiscRequest newRequest = createNewRequest(billCycle, cutoffDate);
        saveRequestAndDiscountContracts(newRequest);
        return newRequest;
    }
    
    

	/** Persists eligible contracts into a temporary table and returns the number of rows inserted. */
	private int persistEligibleContracts(String billCycle, LocalDateTime cutoffDate) {
	    contractRepo.truncateTempTable();
	    log.info("Temporary table truncated before persisting eligible contract");
	    
	    int insertedRows = contractRepo.persistEligibleContractsToTempTable( cutoffDate,billCycle);
	    log.info("Persisted {} eligible contracts into the temporary table for bill cycle '{}' and cutoff date '{}'.",
	            insertedRows, billCycle, cutoffDate);
	    return insertedRows;
	}
	
    /** Creates a new discount request. */
    private DynDiscRequest createNewRequest(String billCycle, LocalDateTime cutoffDate) {
        DynDiscRequest request = new DynDiscRequest();
        Integer nextRequestId = requestRepo.getNextAvailableRequestId();
        request.setRequestId(nextRequestId);
        request.setStatus("W");        
        request.setBillcycle(billCycle);
        request.setBillPeriodEndDate(cutoffDate);
        log.info("New discount request created with ID '{}'.", nextRequestId);
        return request;
    }
	
	/** Inserts the request and moves contracts from the temporary table to the final table. */
	public void saveRequestAndDiscountContracts(DynDiscRequest request) {
	    log.info("Starting transaction for request ID '{}'.", request.getRequestId());
	    requestRepo.save(request);
	    log.info("Saved request ID '{}'.", request.getRequestId());

	    int updatedRows = contractRepo.insertContractsFromTempTable(request.getRequestId());
	    log.info("Inserted {} contracts into the final table for request ID '{}'.", updatedRows, request.getRequestId());

	    contractRepo.truncateTempTable();
	    log.info("Temporary table truncated after processing request ID '{}'.", request.getRequestId());
	}
	


	/** Finalizes the discount request by updating its status and associated statistics. */
    @Transactional
    public void finalizeRequest(Integer requestId) {
        LocalDateTime statusDate = LocalDateTime.now();

        // Determine new status based on failed contracts
        boolean hasFailures = contractRepo.countByReqIdAndStatus(requestId, "F") > 0;
        String newStatus = hasFailures ? "F" : "P";

        // Update request record
        int rows = requestRepo.updateStatusAndEndDate(requestId, newStatus, statusDate);
        if (rows == 0) {
            throw new EntityNotFoundException("Request not found: " + requestId);
        }

        // Refresh and save statistics if no failures
        if (!hasFailures) {
            DynDiscStatistic stats = statisticRepo.getStatsByRequestId(requestId);
            entityManager.detach(stats);
            stats.setEndDate(statusDate);
            statisticRepo.save(stats);
        }
    }

    /** Resets failed contracts for the given request. */
    @Transactional
    public int resetFailedContracts(Integer requestId) {
        int updated = contractRepo.resetFailedContracts(requestId);
        log.info("-> Reset {} failed contracts to 'I' for request {}", updated, requestId);
        return updated;
    }

    /** Retrieves contracts by status for a specific request. */
    public List<DynDiscContract> getContractsByStatus(Integer requestId, String status) {
        List<DynDiscContract> contracts = contractRepo.getContractsByStatus(requestId, status);
        log.info("-> Retrieved {} contracts with status '{}' for request {}", contracts.size(), status, requestId);
        return contracts;
    }

    /** Fetches the ongoing discount request, if any. */
    public DynDiscRequest fetchOngoingRequest() {
        List<DynDiscRequest> ongoingRequests = requestRepo.findByStatus("W");
        return ongoingRequests.isEmpty() ? null : ongoingRequests.get(0);
    }

	public DynDiscRequest fetchRequestById(int requestId) {
	    return requestRepo.findById(requestId).orElse(null);	    	
	}
}
