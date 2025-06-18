package com.atos.dynamicdiscount.processor.manager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.dto.DynDiscGrantEvalDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;
import com.atos.dynamicdiscount.processor.service.evaluation.DiscountEvaluationService;
import com.atos.dynamicdiscount.processor.service.granting.DiscountGrantingService;
import com.atos.dynamicdiscount.processor.service.logging.DiscountLogService;

import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractProcessor {


	private final DiscountEvaluationService evalService;
	private final DiscountGrantingService grantService;
	private final DiscountLogService logService;

	/**
	 * Full async pipeline for a single contract: 1) evaluate 2) grant 3) record
	 */
    
	
    @Retryable(
            value = { SQLException.class ,PersistenceException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2))
	@Transactional
	public void processContract(DynDiscRequest request, DynDiscContract contract,
			List<DynDiscAssignDTO> discounts) {

		Integer coId = contract.getCoId();
		LocalDateTime cutoff = request.getBillPeriodEndDate();
		String assignIds = discounts.stream().map(DynDiscAssignDTO::getAssignId).map(Object::toString)
				.collect(Collectors.joining(", "));
		
				log.info("-------------- Processing coId={} --------------", coId);
				log.info("> coId={} : evaluating {} discounts (AssignIds={})", coId, discounts.size(), assignIds);
							
				// 1) Evaluate discounts for the contract
				DynDiscGrantEvalDTO discGrantEval = evalService.evaluateDiscounts(contract, discounts, cutoff);

				// 2) Grant discounts if evaluation produced valid grant and eval data
				if (discGrantEval != null && discGrantEval.getDynDiscGrantHistory() != null && discGrantEval.getDynDiscEvalHistory() != null) {
					DynDiscGrantHistory grant = discGrantEval.getDynDiscGrantHistory();
					DynDiscEvalHistory eval = discGrantEval.getDynDiscEvalHistory();
					grantService.grantDiscount(eval, grant);
				} else {
					log.warn("! coId={} : Discount grant skipped", coId);
				}
				
				// 3) Record the discount lifecycle event
				logService.recordDiscountLifecycle(discGrantEval, cutoff);
				log.info("< coId={} : processing complete", coId); // Indicate successful processing
			}
    
    
    @Recover
    public void recover(SQLException e, DynDiscContract contract) throws SQLException {
        log.error("Retries exhausted for contract: {}", contract.getCoId(), e);
        throw e; // Rethrow the exception to propagate it further
    }
}
