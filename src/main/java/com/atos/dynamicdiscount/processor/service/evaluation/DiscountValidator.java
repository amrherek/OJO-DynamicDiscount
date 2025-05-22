package com.atos.dynamicdiscount.processor.service.evaluation;

import static java.util.Collections.emptyList;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscCustGrpExclId;
import com.atos.dynamicdiscount.model.entity.DynDiscOffer;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountValidator {

	private final DynDiscConfigurations cfg;

	/**
	 * Extracted and centralized all validity checks from DiscountProcessor.
	 */
	public boolean isValid(DynDiscAssignDTO dto, LocalDateTime cutoffDate) {
		DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());
		if (conf == null) {
			log.warn("! AssignId {}: No config for DiscId {}.", dto.getAssignId(), dto.getDiscId());
			return false;
		}
		if (conf.getValidTo() != null && !conf.getValidTo().isAfter(cutoffDate)) {
			log.info("! AssignId {}: Expired on {}.", dto.getAssignId(), conf.getValidTo());
			return false;
		}
		Integer applied = dto.getApplyCount() != null ? dto.getApplyCount().intValue() : 0;
		if (conf.getDuration() != null && conf.getDuration() != -1 && applied >= conf.getDuration()) {
			log.info("! AssignId {}: Limit reached.", dto.getAssignId());
			return false;
		}
		DynDiscCustGrpExclId exclId = new DynDiscCustGrpExclId(dto.getDiscId().intValue(), dto.getPrgcode());
		if (cfg.getDynDiscCustGrpExclMap().containsKey(exclId)) {
			log.info("! AssignId {}: Discount not allowed - Customer price group '{}' is excluded.", dto.getAssignId(),
					dto.getPrgcode());

			return false;
		}
		List<DynDiscOffer> offers = cfg.getDynDiscOfferMap().getOrDefault(dto.getDiscId().intValue(), emptyList());
		boolean eligible = offers.stream().anyMatch(
				o -> o.getTmcode().equals(dto.getTmCode()) && o.getSncode().equals(dto.getOfferSnCode().intValue()));
		if (!eligible) {
			log.warn("! AssignId {}: TM/SN ({}/{}) not eligible for DiscId {}.", dto.getAssignId(), dto.getTmCode(),

					dto.getOfferSnCode(), dto.getDiscId());
			return false;
		}

		// Suspenstion validation
		if ("S".equals(String.valueOf(dto.getOfferStatus())) && !conf.getSuspInd()) {

			log.info("! AssignId {}: Discount not allowed - Offer Eligible, Status: 'Suspended'. SNCode: {}",
					dto.getAssignId(), dto.getOfferSnCode());
			return false;
		}

		if ("O".equals(String.valueOf(dto.getOfferStatus()))) {
			log.info("! AssignId {}: Discount not allowed - Offer Eligible, Status: 'On Hold'. SNCode: {}",
					dto.getAssignId(), dto.getOfferSnCode());
			return false;
		}

		log.info("✓ AssignId {}: Valid discount.", dto.getAssignId());
		return true;
	}
}
