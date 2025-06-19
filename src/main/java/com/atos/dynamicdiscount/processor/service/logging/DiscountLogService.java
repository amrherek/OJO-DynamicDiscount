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
     * Records the full discount lifecycle by saving contract, evaluation history,
     * grant history, and assignment updates.
     */
    public void recordDiscountLifecycle(DynDiscGrantEvalDTO result, LocalDateTime cutoffDate) {
        DynDiscContract contract = result.getDynDiscContract();
        DynDiscEvalHistory eval = result.getDynDiscEvalHistory();
        DynDiscGrantHistory grant = result.getDynDiscGrantHistory();

        if (contract == null) {
            log.error("DynDiscContract missing; skipping lifecycle recording.");
            return;
        }

        if (eval != null && grant != null) {
            updateContractStatusAndRemark(contract, grant);
            saveEvaluationHistory(eval);
            saveGrantHistory(grant);
            updateAndSaveAssignment(grant, cutoffDate);
            logGrantDetails(contract, grant);
        }

        saveContract(contract);
    }

    private void updateContractStatusAndRemark(DynDiscContract contract, DynDiscGrantHistory grant) {
        boolean offerOccFailed = Boolean.FALSE.equals(grant.getOfferOccCreated()) && grant.getOfferDiscAmount() != 0;
        boolean aloOccFailed = Boolean.FALSE.equals(grant.getAloOccCreated()) && grant.getAloDiscAmount() != 0;

        contract.setStatus((offerOccFailed || aloOccFailed) ? "F" : "P");
        contract.setRemark(createContractRemark(grant, offerOccFailed, aloOccFailed));
    }

    private String createContractRemark(DynDiscGrantHistory grant, boolean offerOccFailed, boolean aloOccFailed) {
        if (!offerOccFailed && !aloOccFailed) {
            String grantedDetails = Boolean.TRUE.equals(grant.getOfferOccCreated()) && Boolean.TRUE.equals(grant.getAloOccCreated())
                    ? "Offer and ALO OCCs granted"
                    : Boolean.TRUE.equals(grant.getOfferOccCreated()) ? "Offer OCC granted"
                    : "ALO OCC granted";
            return String.format("AssignId=%d: Applied (%s).", grant.getAssignId(), grantedDetails);
        }
        
        String failureDetails = (offerOccFailed ? "Offer OCC creation failure" : "") +
                (offerOccFailed && aloOccFailed ? " and " : "") +
                (aloOccFailed ? "ALO OCC creation failure" : "");

        return String.format("AssignId=%d: Failed (%s).", grant.getAssignId(), failureDetails);
    }
    
    

    private void saveContract(DynDiscContract contract) {
        try {
            contractRepo.save(contract);
            log.debug("DynDiscContract [{}] saved.", contract.getCoId());
        } catch (Exception e) {
            log.error("Error saving DynDiscContract [{}]: {}", contract.getCoId(), e);
            throw e;
        }
    }

    private void saveEvaluationHistory(DynDiscEvalHistory eval) {
        try {
            evalRepo.save(eval);
            log.debug("DynDiscEvalHistory [{}] saved.", eval.getAssignId());
        } catch (Exception e) {
            log.error("Error saving DynDiscEvalHistory [{}]: {}", eval.getAssignId(), e);
            throw e;
        }
    }

    private void saveGrantHistory(DynDiscGrantHistory grant) {
        try {
            grantRepo.save(grant);
            log.debug("DynDiscGrantHistory [{}] saved.", grant.getAssignId());
        } catch (Exception e) {
            log.error("Error saving DynDiscGrantHistory [{}]: {}", grant.getAssignId(), e);
            throw e;
        }
    }

    private void updateAndSaveAssignment(DynDiscGrantHistory grant, LocalDateTime cutoffDate) {
        try {
            DynDiscAssign assign = assignRepo.findById(grant.getAssignId())
                    .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + grant.getAssignId()));

            assign.setLastAppliedDate(cutoffDate);
            assign.setApplyCount(grant.getCurrentApplyCount());

            if (Boolean.TRUE.equals(grant.getLastApply())) {
                assign.setExpireDate(cutoffDate);
            }

            assignRepo.save(assign);
            log.debug("DynDiscAssign [{}] updated.", assign.getAssignId());
        } catch (Exception e) {
            log.error("Error updating DynDiscAssign [{}]: {}", grant.getAssignId(), e);
            throw e;
        }
    }

    private void logGrantDetails(DynDiscContract contract, DynDiscGrantHistory grant) {
        log.info(
                "✓ coId={} : Grant Details = [RequestId={}, AssignId={}, OfferDiscAmount={}, FreeMonth={}, SpecialMonth={}, OfferCapped={}, CurrentApplyCount={}, LastApply={}, AloDiscAmount={}, AloDiscInd={}, AloCapped={}, Note={}, OfferOccCreated={}, AloOccCreated={}]",
                contract.getCoId(), grant.getRequestId(), grant.getAssignId(), grant.getOfferDiscAmount(),
                grant.getFreeMonth(), grant.getSpecialMonth(), grant.getOfferCapped(), grant.getCurrentApplyCount(),
                grant.getLastApply(), grant.getAloDiscAmount(), grant.getAloDiscInd(), grant.getAloCapped(),
                grant.getNote(), grant.getOfferOccCreated(), grant.getAloOccCreated());
    }
}
