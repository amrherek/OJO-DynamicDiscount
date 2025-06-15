package com.atos.dynamicdiscount.processor.service.logging;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

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
	 * Records the full discount lifecycle: updates contract, logs evaluation and
	 * grant history, and updates assignment.
	 * 
	 * @param result     DTO containing contract, evaluation, and grant information.
	 * @param cutoffDate The date used for the processing cutoff.
	 */
	// @Transactional
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
			boolean offerOccFailed = Boolean.FALSE.equals(grant.getOfferOccCreated())
					&& grant.getOfferDiscAmount() != 0;
			boolean aloOccFailed = Boolean.FALSE.equals(grant.getAloOccCreated()) && grant.getAloDiscAmount() != 0;
			contract.setStatus((offerOccFailed || aloOccFailed) ? "F" : "P");
			String remark;
			if (contract.getStatus().equals("P")) {
				boolean offerOccGranted = Boolean.TRUE.equals(grant.getOfferOccCreated());
				boolean aloOccGranted = Boolean.TRUE.equals(grant.getAloOccCreated());
				String grantedDetails = (offerOccGranted && aloOccGranted) ? "both Offer and ALO OCCs granted"
						: offerOccGranted ? "Offer OCC granted" : aloOccGranted ? "ALO OCC granted" : "no OCC granted";
				remark = "AssignId=" + grant.getAssignId() + ": Successfully applied (" + grantedDetails + ").";
			} else {
				remark = "AssignId=" + grant.getAssignId() + ": Failed due to: "
						+ (offerOccFailed ? "Offer OCC creation failure" : "")
						+ (offerOccFailed && aloOccFailed ? " and " : "")
						+ (aloOccFailed ? "ALO OCC creation failure" : "") + ".";
			}

			contract.setRemark(remark);
		}

		else {
			log.debug("DynDiscGrantHistory missing; No grant recorded");
		}

		try {
			contractRepo.save(contract);
			log.debug("DynDiscContract [{}] saved.", contract.getCoId());
		} catch (Exception e) {

			log.error("Error saving DynDiscContract [{}]: {}", contract.getCoId(), e);
			throw e;
		}

		// 2. Log Evaluation History
		if (eval != null) {
			try {
				evalRepo.save(eval);
				log.debug("DynDiscEvalHistory [{}] saved.", eval.getAssignId());
			} catch (Exception e) {
				log.error("Error saving DynDiscEvalHistory [{}]: {}", eval.getAssignId(), e);
				throw e;
			}
		}

		// 3. Log Grant History
		if (grant != null) {

			try {
				grantRepo.save(grant);
				log.debug("DynDiscGrantHistory [{}] saved.", grant.getAssignId());
			} catch (Exception e) {
				log.error("Error saving DynDiscGrantHistory [{}]: {}", grant.getAssignId(), e);
				throw e;
			}

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
				log.error("Error saving DynDiscAssign [{}]: {}", grant.getAssignId(), e);
				throw e;
			}
		}

		// 5. Grant Confirmation Log
		if (grant != null) {
			log.info(
					"✓ coId={} : Grant Details = [RequestId={}, AssignId={}, OfferDiscAmount={}, FreeMonth={}, SpecialMonth={}, OfferCapped={}, CurrentApplyCount={}, LastApply={}, AloDiscAmount={}, AloDiscInd={}, AloCapped={}, Note={}, OfferOccCreated={}, AloOccCreated={}]",
					contract.getCoId(), grant.getRequestId(), grant.getAssignId(), grant.getOfferDiscAmount(),
					grant.getFreeMonth(), grant.getSpecialMonth(), grant.getOfferCapped(), grant.getCurrentApplyCount(),
					grant.getLastApply(), grant.getAloDiscAmount(), grant.getAloDiscInd(), grant.getAloCapped(),
					grant.getNote(), grant.getOfferOccCreated(), grant.getAloOccCreated());
		}
	}
}