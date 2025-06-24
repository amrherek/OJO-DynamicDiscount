package com.atos.dynamicdiscount.processor.service.granting;

import java.sql.SQLException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscountGrantingService {

    private final JdbcTemplate jdbcTemplate;

    // Flag to control procedure call
    @Value("${occ.grant.enabled:true}")
    private boolean isGrantOccEnabled;

    /**
     * Grants offer and ALO discounts by calling the BSCS stored procedure.
     */
    public void grantDiscount(DynDiscEvalHistory eval, DynDiscGrantHistory grant) {
        // Grant Offer discount
        grantDiscountIfValid(eval, grant, "Offer", grant.getOfferDiscAmount(), "");

        // Grant ALO discount if the indicator is true
        if (Boolean.TRUE.equals(grant.getAloDiscInd())) {
            grantDiscountIfValid(eval, grant, "ALO", grant.getAloDiscAmount(), " ALO");
        }
    }

    /**
     * Helper method to grant a discount if the amount is valid.
     */
    private void grantDiscountIfValid(DynDiscEvalHistory eval, DynDiscGrantHistory grant,
                                      String discountType, Float amount, String remarkSuffix) {
        if (amount != null && amount > 0) {
            grantOcc(eval, grant, remarkSuffix, discountType, amount);
        } else {
            setOccCreatedFlag(grant, discountType, false);
            log.info("- coId={} : {} OCC not created (AssignId={}): Zero/null amount.",
                      eval.getCoId(), discountType, eval.getAssignId());
        }
    }

    /**
     * Helper method to call the stored procedure for granting a discount OCC.
     */
    private void grantOcc(DynDiscEvalHistory eval, DynDiscGrantHistory grant,
                          String remarkSuffix, String discountType, Float amount) {

        boolean success = false;
        try {
            if (!isGrantOccEnabled) {
                log.info("✗ OCC grant skipped as occ.grant.enabled is set to false (AssignId={})", eval.getAssignId());
                success = true;
                return;
            }

            LocalDateTime validFrom = eval.getBillPeriodEndDate().minusDays(1);
            String remark = eval.getOccRemark() + remarkSuffix;

            long startTime = System.currentTimeMillis();

            jdbcTemplate.update(
                "CALL bscs_wd.mcd_wan_pkg.man_addocc(?, ?, ?, ?, ?, ?, ?, ?, ?)",
                eval.getCustomerId(), eval.getCoId(), validFrom, amount * -1,
                remark, eval.getOccGlcode(), eval.getOccSncode(), eval.getTmCode(), validFrom
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("✓ coId={} : {} OCC grant completed in {} ms (AssignId={})",
                     eval.getCoId(), discountType, duration, eval.getAssignId());

            success = true;
        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                log.error("✗ coId={} : {} OCC grant failed with SQLException (AssignId={})",
                          eval.getCoId(), discountType, eval.getAssignId(), ex);
                throw ex; // Propagate SQLExceptions
            } else {
                log.error("✗ coId={} : {} OCC grant failed with non-SQLException (AssignId={})",
                          eval.getCoId(), discountType, eval.getAssignId(), ex);
            }
        } finally {
            setOccCreatedFlag(grant, discountType, success);
        }
    }

    /**
     * Sets the OCC created flag for the given discount type.
     */
    private void setOccCreatedFlag(DynDiscGrantHistory grant, String discountType, boolean success) {
        if ("Offer".equals(discountType)) {
            grant.setOfferOccCreated(success);
        } else if ("ALO".equals(discountType)) {
            grant.setAloOccCreated(success);
        }
    }
}
