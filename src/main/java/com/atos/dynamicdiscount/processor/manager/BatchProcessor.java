package com.atos.dynamicdiscount.processor.manager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.repository.DynDiscAssignRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessor {


	private final DynDiscAssignRepository assignRepository;
	private final ContractProcessor contractProcessor;
	

	/**
	 * Split contracts into batches, fetch discounts once per batch, and hand each
	 * contract off to the contractService.
	 */
	public void processInBatches(DynDiscRequest request, List<DynDiscContract> allContracts, int batchSize) {

		log.info("Starting batch processing of {} contracts with a batch size of {}", allContracts.size(), batchSize);
		LocalDateTime cutoff = request.getBillPeriodEndDate();
		Integer requestId = request.getRequestId();

		for (int i = 0; i < allContracts.size(); i += batchSize) {
			int end = Math.min(i + batchSize, allContracts.size());
			List<DynDiscContract> batch = allContracts.subList(i, end);

			log.info("→ Processing batch {}–{} (size={})", i, end, batch.size());
			List<Integer> coIds = batch.stream().map(c -> c.getCoId()).collect(Collectors.toList());

			// fetch and group by contract
			List<DynDiscAssignDTO> allDiscounts = assignRepository.fetchAssignedDiscounts(coIds, requestId, cutoff);
			Map<Integer, List<DynDiscAssignDTO>> discountsByCoId = allDiscounts.stream()
					.collect(Collectors.groupingBy(dto -> dto.getCoId().intValue()));
			

			// trigger each contract’s pipeline
			List<CompletableFuture<Void>> futures = batch.stream()
					.map(c -> contractProcessor.processContract(request, c,
							discountsByCoId.getOrDefault(c.getCoId(), Collections.emptyList())))
					.collect(Collectors.toList());

			CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			log.info("✓ Completed batch of {} contracts", batch.size());
		}
			
		

		log.info("All {} contracts processed", allContracts.size());
	}
}
