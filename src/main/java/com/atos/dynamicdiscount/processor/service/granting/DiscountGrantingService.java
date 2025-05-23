package com.atos.dynamicdiscount.processor.service.granting;

import java.time.LocalDateTime;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscountGrantingService {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * Grants offer and ALO discounts by calling the BSCS stored procedure.
	 */
	@Transactional
	public void grantDiscount(DynDiscEvalHistory eval, DynDiscGrantHistory grant) {

		// Grant offer discount if amount is valid
		grantOcc(eval, grant, "", "Offer");

		// Grant ALO discount if indicator is true and amount is valid
		if (Boolean.TRUE.equals(grant.getAloDiscInd()) && grant.getAloDiscAmount() != null
				&& grant.getAloDiscAmount() > 0) {
			grantOcc(eval, grant, " ALO", "ALO");
		}
	}

	/**
	 * Helper method to call the stored procedure for granting a discount. Extracts
	 * the amount from the grant object and updates the grant history with OCC
	 * created flags.
	 */
	private void grantOcc(DynDiscEvalHistory eval, DynDiscGrantHistory grant, String remarkSuffix,
			String discountType) {
		Float amount = null;
		if ("Offer".equals(discountType)) {
			amount = grant.getOfferDiscAmount();
		} else if ("ALO".equals(discountType)) {
			amount = grant.getAloDiscAmount();
		}

		if (amount != null && amount != 0) {
			boolean success = false;
			try {
				LocalDateTime validFrom = eval.getBillPeriodEndDate().minusDays(1);
				String remark = eval.getOccRemark() + remarkSuffix;
				String glcode = eval.getOccGlcode();
				Integer sncode = eval.getOccSncode();
				Integer tmcode = eval.getTmCode();
				Integer customerId = eval.getCustomerId();
				Integer coId = eval.getCoId();

				// Call the stored procedure to grant the OCC
				jdbcTemplate.update("CALL bscs_wd.mcd_wan_pkg.man_addocc(?, ?, ?, ?, ?, ?, ?, ?, ?)", customerId, coId,
						validFrom, amount, remark, glcode, sncode, tmcode, validFrom);

				success = true;
				log.info("✓ coId={} : {} discount OCC granted (AssignId={})", coId, discountType, eval.getAssignId());

			} catch (DataAccessException ex) {
				log.error("✗ coId={} : {} discount OCC grant failed (AssignId={})", eval.getCoId(), discountType,
						eval.getAssignId());
				ex.printStackTrace();
			} finally {
				// Set the OCC created flag based on the success of the stored procedure call
				if ("Offer".equals(discountType)) {
					grant.setOfferOccCreated(success);
				} else if ("ALO".equals(discountType)) {
					grant.setAloOccCreated(success);
				}
			}
		} else {
			// Set OCC created flag to null if no valid amount to process (not created)
			if ("Offer".equals(discountType)) {
				grant.setOfferOccCreated(false);
				log.info("- coId={} : Offer OCC not created (AssignId={}): Zero/null amount.", eval.getCoId(),
						eval.getAssignId());
			} else if ("ALO".equals(discountType)) {
				grant.setAloOccCreated(false);
				log.info("- coId={} : ALO OCC not created (AssignId={}): Zero/null amount.", eval.getCoId(),
						eval.getAssignId());
			}

		}
	}
}