package com.atos.dynamicdiscount.processor.service.logging;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atos.dynamicdiscount.model.dto.DynDiscGrantEvalDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscAssign;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;
import com.atos.dynamicdiscount.repository.DynDiscAssignRepository;
import com.atos.dynamicdiscount.repository.DynDiscContractRepository;
import com.atos.dynamicdiscount.repository.DynDiscEvalHistoryRepository;
import com.atos.dynamicdiscount.repository.DynDiscGrantHistoryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountLogService {

	private final DynDiscContractRepository contractRepo;
	private final DynDiscAssignRepository assignRepo;
	private final DynDiscGrantHistoryRepository grantRepo;
	private final DynDiscEvalHistoryRepository evalRepo;

	/**
	 * Records the full discount lifecycle: updates contract, logs evaluation and grant history, and updates assignment.
	 * @param result DTO containing contract, evaluation, and grant information.
	 * @param cutoffDate The date used for the processing cutoff.
	 */
	@Transactional
	public void recordDiscountLifecycle(DynDiscGrantEvalDTO result, LocalDateTime cutoffDate) {
		// Extract entities
		DynDiscContract contract = result.getDynDiscContract();
		DynDiscEvalHistory eval = result.getDynDiscEvalHistory();
		DynDiscGrantHistory grant = result.getDynDiscGrantHistory();

		// 1. Update DYN_DISC_CONTRACT
		if (contract == null) {
			log.error("DynDiscContract missing; skipping contract update.");
			return;
		}

		if (grant != null) {
			contract.setStatus(!Boolean.FALSE.equals(grant.getOfferOccCreated()) && !Boolean.FALSE.equals(grant.getAloOccCreated()) ? "P" : "F");
			contract.setRemark("AssignId=" + grant.getAssignId() + ": " + (contract.getStatus().equals("P") ? "Successfully applied." : "Either ALO or Offer OCC creation failed."));
		} else {
			log.debug("DynDiscGrantHistory missing; contract status not updated based on grant.");
		}
		contractRepo.save(contract);
		log.debug("DynDiscContract [{}] saved.", contract.getCoId());

		// 2. Log Evaluation History
		if (eval != null) {
			evalRepo.save(eval);
			log.debug("DynDiscEvalHistory [{}] saved.", eval.getAssignId());
		}

		// 3. Log Grant History
		if (grant != null) {
			grantRepo.save(grant);
			log.debug("DynDiscGrantHistory [{}] saved.", grant.getAssignId());

	  // 4. Update DYN_DISC_ASSIGN
			try {
				DynDiscAssign assign = assignRepo.findById(grant.getAssignId())
						.orElseThrow(() -> new EntityNotFoundException("Assign not found: " + grant.getAssignId()));
				assign.setLastAppliedDate(cutoffDate);
				assign.setApplyCount(grant.getCurrentApplyCount());
				if (Boolean.TRUE.equals(grant.getLastApply())) {
					assign.setExpireDate(cutoffDate);
				}
				assignRepo.save(assign);
				log.debug("DynDiscAssign [{}] updated.", assign.getAssignId());
			} catch (EntityNotFoundException e) {
				log.error("Error updating DynDiscAssign: {}", e.getMessage());
			}
		}

		// 5. Grant Confirmation Log
		if (grant != null) {
			log.info("✓ coId={} : Grant Details = [RequestId={}, AssignId={}, OfferDiscAmount={}, FreeMonth={}, SpecialMonth={}, OfferCapped={}, CurrentApplyCount={}, LastApply={}, AloDiscAmount={}, AloDiscInd={}, AloCapped={}, Note={}, OfferOccCreated={}, AloOccCreated={}]",
			        contract.getCoId(),
			        grant.getRequestId(),
			        grant.getAssignId(),
			        grant.getOfferDiscAmount(),
			        grant.getFreeMonth(),
			        grant.getSpecialMonth(),
			        grant.getOfferCapped(),
			        grant.getCurrentApplyCount(),
			        grant.getLastApply(),
			        grant.getAloDiscAmount(),
			        grant.getAloDiscInd(),
			        grant.getAloCapped(),
			        grant.getNote(),
			        grant.getOfferOccCreated(),
			        grant.getAloOccCreated());
		}
	}
}