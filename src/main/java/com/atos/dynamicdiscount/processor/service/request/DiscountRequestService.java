package com.atos.dynamicdiscount.processor.service.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.model.entity.DynDiscStatistic;
import com.atos.dynamicdiscount.repository.DynDiscContractRepository;
import com.atos.dynamicdiscount.repository.DynDiscPackageRepository;
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
    private final DynDiscPackageRepository packageRepo;
    private final DynDiscStatisticRepository statisticRepo;

    @Value("${request.package.size:10000}")
    private int packageSize;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Registers a new discount request for eligible contracts and packages.
     */
    @Transactional
    public DynDiscRequest registerNewRequest(String billCycle, LocalDateTime cutoffDate) {
        log.info("Starting registration of a new discount request for bill cycle '{}' and cutoff date '{}'.", billCycle, cutoffDate);

        // Check if there are eligible contracts
        if (!hasEligibleContracts(billCycle, cutoffDate)) {
            return null;
        }

        // Create and save a new discount request
        DynDiscRequest newRequest = createAndSaveNewRequest(billCycle, cutoffDate);

        // Insert eligible contracts for this request
        insertEligibleContracts(cutoffDate, billCycle, newRequest.getRequestId());

        // Insert packages based on the inserted contracts
        insertPackages(newRequest.getRequestId());

        log.info("Completed registration of discount request ID '{}'.", newRequest.getRequestId());
        return newRequest;
    }

    /**
     * Checks for eligible contracts based on cutoff date and bill cycle.
     */
    private boolean hasEligibleContracts(String billCycle, LocalDateTime cutoffDate) {
        // Query the number of eligible contracts
        int eligibleCount = contractRepo.countEligibleContracts(cutoffDate, billCycle);
        if (eligibleCount == 0) {
            log.warn("No eligible contracts found for bill cycle '{}' and cutoff date '{}'. Registration aborted.", billCycle, cutoffDate);
            return false;
        }
        log.info("Identified {} eligible contracts for bill cycle '{}' and cutoff date '{}'.", eligibleCount, billCycle, cutoffDate);
        return true;
    }

    /**
     * Creates and saves a new discount request.
     */
    private DynDiscRequest createAndSaveNewRequest(String billCycle, LocalDateTime cutoffDate) {
        // Create a new request object
        DynDiscRequest request = new DynDiscRequest();

        // Generate the next request ID
        Integer nextRequestId = requestRepo.getNextAvailableRequestId();

        // Set request properties
        request.setRequestId(nextRequestId);
        request.setStatus("W");
        request.setBillcycle(billCycle);
        request.setBillPeriodEndDate(cutoffDate);

        // Save the request
        requestRepo.save(request);
        log.info("Created and saved a new discount request with ID '{}'.", nextRequestId);
        return request;
    }

    /**
     * Inserts eligible contracts into the database for the specified request.
     */
    private void insertEligibleContracts(LocalDateTime cutoffDate, String billCycle, Integer requestId) {
        log.info("Inserting eligible contracts into the database for request ID '{}'.", requestId);

        // Insert contracts in batches
        contractRepo.insertContracts(cutoffDate, billCycle, requestId, packageSize);

        log.info("Successfully inserted contracts for request ID '{}'.", requestId);
    }

    /**
     * Inserts packages based on the inserted contracts.
     */
    private void insertPackages(Integer requestId) {
        log.info("Inserting packages into the database for request ID '{}'.", requestId);

        // Insert packages linked to the inserted contracts
        packageRepo.insertPackages(requestId);

        log.info("Successfully inserted packages for request ID '{}'.", requestId);
    }

    /**
     * Finalizes the request by updating its status based on the state of its contracts and packages.
     */
    @Transactional
    public void finalizeRequest(Integer requestId) {
        LocalDateTime statusDate = LocalDateTime.now();

        // Check if any contracts are still in initial state
        if (contractRepo.countByReqIdAndStatus(requestId, "I") > 0) {
            log.info("Request ID {}: Finalization skipped as contracts are in an initial state ('I').", requestId);
            return;
        }

        // Check if any packages are still in initial or working state
        if (packageRepo.countByReqIdAndStatus(requestId, "I") > 0 || packageRepo.countByReqIdAndStatus(requestId, "W") > 0) {
            log.info("Request ID {}: Finalization skipped due to ongoing ,initial packages.", requestId);
            return;
        }

        // Determine final status based on the presence of failed contracts
        
        boolean hasFailure = contractRepo.countByReqIdAndStatus(requestId, "F") > 0 
                || packageRepo.countByReqIdAndStatus(requestId, "F") > 0;

        String newStatus = hasFailure ? "F" : "P";

        // Update the request status
        if (requestRepo.updateStatusAndEndDate(requestId, newStatus, statusDate) == 0) {
            throw new EntityNotFoundException("Request not found for ID: " + requestId);
        }

        // If successful, update statistics
        if (!"F".equals(newStatus)) {
            DynDiscStatistic stats = statisticRepo.getStatsByRequestId(requestId);
            entityManager.detach(stats);
            stats.setEndDate(statusDate);
            statisticRepo.save(stats);
        }

        log.info("Request ID {}: Finalization completed with status '{}'.", requestId, newStatus);
    }

    /**
     * Resets failed contracts and packages for the specified request.
     */
    @Transactional
    public Integer resetFailedContractsAndPackages(Integer requestId) {
        // Reset contracts that have failed or are in an initial state
        int updatedContracts = contractRepo.resetFailedAndInitialContracts(requestId);

        if (updatedContracts == 0) {
            log.info("No contracts were reset for request ID {}. Aborting further processing.", requestId);
            return updatedContracts;
        }

        // Update package status to 'P'
        packageRepo.updatePackagesToP(requestId);

        // Assign new pack IDs to updated contracts
        Integer nextPackIdStart = packageRepo.findMaxPackIdByRequestId(requestId) == null ? 1 : packageRepo.findMaxPackIdByRequestId(requestId) + 1;
        contractRepo.assignNewPackIds(requestId, packageSize, nextPackIdStart);

        // Insert new packages for the updated contracts
        packageRepo.insertNewPackagesForUpdatedContracts(requestId);
        
        //
        LocalDateTime statusDate = LocalDateTime.now();
        requestRepo.updateStatusAndEndDate(requestId, "W", statusDate);

        return updatedContracts;
    }

    /**
     * Retrieves contracts by their status for a specified request.
     */
    public List<DynDiscContract> getContractsByStatus(Integer requestId, String status) {
        return contractRepo.getContractsByStatus(requestId, status);
    }

    /**
     * Fetches an ongoing discount request.
     */
    public DynDiscRequest fetchOngoingRequest() {
        List<DynDiscRequest> ongoingRequests = requestRepo.findByStatus("W");
        return ongoingRequests.isEmpty() ? null : ongoingRequests.get(0);
    }

    /**
     * Fetches a discount request by its ID.
     */
    public DynDiscRequest fetchRequestById(int requestId) {
        return requestRepo.findById(requestId).orElse(null);
    }
}
