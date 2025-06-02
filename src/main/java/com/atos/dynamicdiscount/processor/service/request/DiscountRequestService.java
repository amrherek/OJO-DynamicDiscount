package com.atos.dynamicdiscount.processor.service.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.exceptions.MultipleOngoingRequestsException;
import com.atos.dynamicdiscount.model.dto.DynDiscContractDTO;
import com.atos.dynamicdiscount.model.dto.NewRequestResultDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.model.entity.DynDiscStatistic;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;
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
    private final DynDiscConfigurations configurations;

    @PersistenceContext
    private EntityManager entityManager;


   
    /** Registers a new discount request and processes eligible contracts. */
    public NewRequestResultDTO registerNewRequest(String billCycle, LocalDateTime cutoffDate) {
        configurations.refreshConfigurations();
        log.info("Fetching eligible contracts for bill cycle '{}' and cutoff date '{}'.", billCycle, cutoffDate);
        List<DynDiscContractDTO> eligibleContracts = fetchEligibleContracts(billCycle, cutoffDate);

        if (eligibleContracts.isEmpty()) {
            log.warn("No eligible contracts were found for bill cycle '{}' and cutoff date '{}'.", billCycle, cutoffDate);
            return null;
        }

        DynDiscRequest newRequest = createNewRequest(billCycle, cutoffDate);
        List<DynDiscContract> discountContracts = mapToDynDiscContracts(eligibleContracts, newRequest);
        saveRequestAndDiscountContracts(newRequest, discountContracts);
        return new NewRequestResultDTO(newRequest, discountContracts);
    }

    /** Fetches eligible contracts for the given bill cycle and cutoff date. */
    private List<DynDiscContractDTO> fetchEligibleContracts(String billCycle, LocalDateTime cutoffDate) {
        List<DynDiscContractDTO> eligibleContracts = contractRepo.fetchEligibleContracts(cutoffDate, billCycle);
        log.info("Retrieved {} eligible contracts for bill cycle '{}' and cutoff date '{}'.", eligibleContracts.size(),
                billCycle, cutoffDate);
        return eligibleContracts;
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

    /** Maps eligible contracts to discount contract entities. */
    private List<DynDiscContract> mapToDynDiscContracts(List<DynDiscContractDTO> eligibleContracts,
                                                        DynDiscRequest newRequest) {
        List<DynDiscContract> dynDiscContracts = new ArrayList<>();
        Set<BigDecimal> processedCoIds = new HashSet<>();

        for (DynDiscContractDTO eligibleContract : eligibleContracts) {
            BigDecimal coId = eligibleContract.getCoId();
            if (processedCoIds.add(coId)) {
                DynDiscContract dynDisContract = new DynDiscContract();
                dynDisContract.setRequestId(newRequest.getRequestId().intValue());
                dynDisContract.setCustomerId(eligibleContract.getCustomerId().intValue());
                dynDisContract.setCoId(coId.intValue());
                dynDisContract.setLbcDate(eligibleContract.getLbcDate());
                dynDisContract.setPrgcode(eligibleContract.getPrgcode());
                dynDisContract.setTmcode(eligibleContract.getTmcode().intValue());
                dynDisContract.setStatus("I");

                dynDiscContracts.add(dynDisContract);
                log.debug("Mapped eligible contract with CO ID '{}' to DynDiscContract for request ID '{}'.", coId,
                        newRequest.getRequestId());
            } else {
                log.debug("Skipping eligible contract with CO ID '{}' as it has already been processed for request ID '{}'.",
                        coId, newRequest.getRequestId());
            }
        }
        log.debug("Mapped {} eligible contracts to {} discount contracts for request ID '{}'.",
                eligibleContracts.size(), dynDiscContracts.size(), newRequest.getRequestId());
        return dynDiscContracts;
    }

    /** Saves the request and its associated discount contracts. */
    @Transactional
    private void saveRequestAndDiscountContracts(DynDiscRequest request, List<DynDiscContract> discountContracts) {
        requestRepo.save(request);
        contractRepo.saveAll(discountContracts);
        log.info("Successfully saved discount request ID '{}' and {} associated contracts.", request.getRequestId(),
                discountContracts.size());
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
    public void resetFailedContracts(Integer requestId) {
        int updated = contractRepo.resetFailedContracts(requestId);
        log.info("-> Reset {} failed contracts to 'I' for request {}", updated, requestId);
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
