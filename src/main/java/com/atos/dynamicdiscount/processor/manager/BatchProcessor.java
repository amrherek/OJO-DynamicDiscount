package com.atos.dynamicdiscount.processor.manager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.repository.DynDiscAssignRepository;
import com.atos.dynamicdiscount.repository.DynDiscContractRepository;
import com.atos.dynamicdiscount.repository.DynDiscPackageRepository;
import com.google.common.collect.Lists;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BatchProcessor is responsible for managing and processing packages and their associated contracts.
 * It processes packages in parallel up to a defined limit, splits contracts into chunks,
 * and processes each chunk asynchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessor {

    private final DynDiscAssignRepository assignRepository;
    private final DynDiscPackageRepository packageRepo;
    private final DynDiscContractRepository contractRepo;
    private final ContractProcessor contractProcessor;
    private final ThreadPoolTaskExecutor executor;

    // Reflects concurrency limit for packages
    @Value("${processing.max.concurrent.packages:10}")
    private int maxConcurrentPackages;
    
 // Reflects the concurrency limit for chunks per package
    @Value("${processing.max.concurrent.chunks:10}")
    private int maxConcurrentChunks;

    // Reflects the number of contracts processed per chunk
    @Value("${processing.contracts.per.chunk:1000}")
    private int contractsPerChunk;

    

    /**
     * Processes all available packages for a given request. Packages are processed concurrently,
     * limited by the maximum number of parallel packages allowed.
     */
    public void processRequestPackages(DynDiscRequest request) {
        Integer requestId = request.getRequestId();
        List<Integer> packageIds = packageRepo.fetchAvailablePackagesWithStatus(requestId, "I");

        if (packageIds.isEmpty()) {
            log.info("No packages with status 'I' available for request ID: {}", requestId);
            return;
        }

        log.info("Found {} packages to process for request ID: {}. Processing up to {} in parallel.",
                packageIds.size(), requestId, maxConcurrentPackages);

        // Semaphore to limit the number of concurrently processed packages
        Semaphore semaphore = new Semaphore(maxConcurrentPackages);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Integer packageId : packageIds) {
            semaphore.acquireUninterruptibly(); // Acquire a permit before processing a package
            CompletableFuture<Void> future = processPackage(packageId, request)
                    .whenComplete((result, ex) -> semaphore.release()); // Release the permit after completion
            futures.add(future);
        }

        // Wait for all packages to finish processing
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("All package processing has been completed for request ID: {}", requestId);
    }

    /**
     * Processes a single package, including loading discounts, splitting contracts into chunks,
     * and processing each chunk asynchronously.
     */
    private CompletableFuture<Void> processPackage(Integer packageId, DynDiscRequest request) {
        return CompletableFuture.runAsync(() -> {
            log.info("→ Starting processing for package ID: {}", packageId);
            try {
                packageRepo.updatePackageStatusAndStartDate(packageId, "W", LocalDateTime.now());

                Map<Integer, List<DynDiscAssignDTO>> discountsByCoId = loadDiscountsByCoId(packageId, request);
                List<DynDiscContract> packageContracts = contractRepo.fetchContractsForPackage(packageId, request.getRequestId());

                if (packageContracts.isEmpty()) {
                    handleEmptyPackage(packageId);
                    return;
                }

                log.info("Package ID: {}. Loaded {} contracts and {} discount assignments.",
                        packageId, packageContracts.size(), discountsByCoId.values().stream().mapToInt(List::size).sum());

                processContractChunks(packageId, request, discountsByCoId, packageContracts);
                packageRepo.updatePackageStatusAndEndDate(packageId, "P", LocalDateTime.now());
                log.info("✓ Successfully completed processing for package ID: {}", packageId);
            } catch (Exception e) {
                handlePackageProcessingError(packageId, e);
            }
        }, executor);
    }

    private Map<Integer, List<DynDiscAssignDTO>> loadDiscountsByCoId(Integer packageId, DynDiscRequest request) {
        List<DynDiscAssignDTO> assignedDiscounts = assignRepository.fetchDiscountsByPackage(
                packageId, request.getRequestId(), request.getBillPeriodEndDate());
        return assignedDiscounts.stream()
                .collect(Collectors.groupingBy(dto -> dto.getCoId().intValue()));
    }

    private void handleEmptyPackage(Integer packageId) {
        log.warn("No contracts found for package ID: {}. Marking as processed.", packageId);
        packageRepo.updatePackageStatusAndEndDate(packageId, "P", LocalDateTime.now());
    }

	private void processContractChunks(Integer packageId, DynDiscRequest request,
			Map<Integer, List<DynDiscAssignDTO>> discountsByCoId, List<DynDiscContract> packageContracts) {
		List<List<DynDiscContract>> contractChunks = Lists.partition(packageContracts, contractsPerChunk);
		log.info("Package ID: {}. Splitting {} contracts into {} chunks of size {}.", packageId,
				packageContracts.size(), contractChunks.size(), contractsPerChunk);

		Semaphore chunkSemaphore = new Semaphore(maxConcurrentChunks);
		List<CompletableFuture<Void>> chunkFutures = new ArrayList<>();

		for (List<DynDiscContract> chunk : contractChunks) {
			try {
				// Attempt to acquire semaphore before processing a chunk
				log.debug("Acquiring semaphore for processing chunk of size {} in package ID: {}", chunk.size(),
						packageId);
				chunkSemaphore.acquireUninterruptibly();

				CompletableFuture<Void> future = processContractChunk(chunk, request, discountsByCoId, packageId)
						.whenComplete((result, ex) -> {
							// Release semaphore after processing or on exception
							chunkSemaphore.release();
							if (ex != null) {
								log.error("Error processing chunk in package ID: {}. Details: {}", packageId,
										ex.getMessage(), ex);
							}
						});
				chunkFutures.add(future);

			} catch (Exception e) {
				// 	Handle unexpected exceptions and ensure semaphore release
				log.error(
						"Critical error while acquiring semaphore or processing chunk for package ID: {}. Details: {}",
						packageId, e.getMessage(), e);
				chunkSemaphore.release();
				throw e; // Re-throw to escalate error to the caller
			}
		}

		// Wait for all chunks to complete processing
		try {
			CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).join();
			log.info("All chunks successfully processed for package ID: {}", packageId);
		} catch (Exception e) {
			log.error("Critical error while waiting for all chunks to complete for package ID: {}. Details: {}",
					packageId, e.getMessage(), e);
			throw e; // Escalate to caller for global handling
		}
	}

    private void handlePackageProcessingError(Integer packageId, Exception e) {
        log.error("✗ Critical error processing package ID: {}. Package will be marked as 'F' (Failed).", packageId, e);
        packageRepo.updatePackageStatusAndEndDate(packageId, "F", LocalDateTime.now());
    }

    
    private CompletableFuture<Void> processContractChunk(
            List<DynDiscContract> contractChunk,
            DynDiscRequest request,
            Map<Integer, List<DynDiscAssignDTO>> discountsByCoId,
            Integer packageId) {
        return CompletableFuture.runAsync(() -> {
            log.info("→ Processing chunk of {} contracts for package ID: {}", contractChunk.size(), packageId);
            for (DynDiscContract contract : contractChunk) {
                try {
                    List<DynDiscAssignDTO> discounts = discountsByCoId.getOrDefault(contract.getCoId(), Collections.emptyList());
                    contractProcessor.processContract(request, contract, discounts);
                } catch (Exception e) {
                    handleContractProcessingError(request.getRequestId(),contract.getCoId(), packageId, e);
                }
            }
            log.info("✓ Finished processing chunk for package ID: {}", packageId);
        }, executor);
    }
    
    
    private void handleContractProcessingError(Integer requestId, Integer coId, Integer packageId, Exception e) {
        log.error("✗ Error processing contract coId={} in package ID={}: {}", coId, packageId, e.getMessage(), e);
        String remark = String.format("Error processing contract. Reason: %s.", e != null ? e.getMessage() : "Unknown error");
        contractRepo.updateContractStatusAndRemark(requestId, coId, "F", remark);
    }

    
    
    
}