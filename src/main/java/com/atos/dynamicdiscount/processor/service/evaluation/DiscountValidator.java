package com.atos.dynamicdiscount.processor.service.evaluation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscOffer;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroup;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountValidator {

    private final DynDiscConfigurations cfg;

    /**
     * Determines if a discount assignment is valid based on various criteria.
     */
    public boolean isValid(DynDiscAssignDTO dto, LocalDateTime cutoffDate, StringBuilder error) {
        if (!hasValidConfig(dto, cutoffDate, error)) {
            return false;
        }
        if (isPriceGroupExcluded(dto, error)) {
            return false;
        }
        if (!isOfferEligible(dto, error)) {
            return false;
        }
        if (!isOfferStatusPermitted(dto, error)) {
            return false;
        }
        log.info("✓ AssignId {}: Valid discount.", dto.getAssignId());
        return true;
    }

    /**
     * Checks if the discount configuration exists, is active, and its application limit has not been reached.
     */
    private boolean hasValidConfig(DynDiscAssignDTO dto, LocalDateTime cutoffDate, StringBuilder error) {
        DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());
        if (conf == null) {
            String errMsg = String.format("! AssignId %s: No config for DiscId %s.", dto.getAssignId(), dto.getDiscId());
            log.warn(errMsg);
            error.append(errMsg);
            return false;
        }

        if ((conf.getValidTo() != null && !conf.getValidTo().isAfter(cutoffDate))
                || (conf.getValidFrom() != null && !conf.getValidFrom().isBefore(cutoffDate))) {
            String errMsg = String.format(
                "AssignId %s: Discount expired, not valid for the cutoff date %s (Valid From: %s, Valid To: %s).",
                dto.getAssignId(), cutoffDate, conf.getValidFrom(), conf.getValidTo());
            log.info(errMsg);
            error.append(errMsg);
            return false;
        }

        int applied = dto.getApplyCount() != null ? dto.getApplyCount().intValue() : 0;
        int limit = (dto.getOvwApplyCount() != null) ? dto.getOvwApplyCount().intValue()
                : (conf.getDuration() != null ? conf.getDuration() : -1);
        if (limit != -1 && applied >= limit) {
            String errMsg = String.format("! AssignId %s: Limit reached.", dto.getAssignId());
            log.info(errMsg);
            error.append(errMsg);
            return false;
        }
        return true;
    }

    /**
     * Checks if the customer's price group is excluded or restricted from the discount.
     */
    private boolean isPriceGroupExcluded(DynDiscAssignDTO dto, StringBuilder error) {
        List<DynDiscPriceGroup> groups = cfg.getDynDiscPriceGroupMap().values().stream()
            .filter(pg -> pg.getId().getDiscId().equals(dto.getDiscId().intValue()))
            .filter(pg -> pg.isRestrictInd() != pg.isProhibitInd())
            .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return false;
        }

        Set<String> restricted = groups.stream()
            .filter(DynDiscPriceGroup::isRestrictInd)
            .map(pg -> pg.getId().getPrgcode())
            .collect(Collectors.toSet());
        
        
        
     // Check restricted list first
        if (!restricted.isEmpty()) {
            if (!restricted.contains(dto.getPrgcode())) {
                String errMsg = String.format(
                    "! AssignId %s: Discount restricted - Customer price group '%s' not allowed.",
                    dto.getAssignId(), dto.getPrgcode());
                log.info(errMsg);
                error.append(errMsg);
                return true;
            }
            // If restricted list is not empty and we get here, 
            // it means the prgcode is in restricted list (allowed)
            return false;
        }
        
        
        // ONLY check prohibited list if restricted list is empty
        Set<String> prohibited = groups.stream()
            .filter(DynDiscPriceGroup::isProhibitInd)
            .map(pg -> pg.getId().getPrgcode())
            .collect(Collectors.toSet());
        if (!prohibited.isEmpty() && prohibited.contains(dto.getPrgcode())) {
            String errMsg = String.format(
                "! AssignId %s: Discount prohibited - Customer price group '%s' is excluded.",
                dto.getAssignId(), dto.getPrgcode());
            log.info(errMsg);
            error.append(errMsg);
            return true;
        }
        return false;
    }
        
        
        
        
       
    /**
     * Checks if the discount offer is eligible based on TMCode and SNCode matching.
     */
    private boolean isOfferEligible(DynDiscAssignDTO dto, StringBuilder error) {
    	
    	
    	// Create formatter for "YYYYMMDDHH24MISS" pattern
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    	
        LocalDateTime assignDate = dto.getAssignDate().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        List<DynDiscOffer> offers = cfg.getDynDiscOfferMap()
            .getOrDefault(dto.getDiscId().intValue(), Collections.emptyList());

        for (DynDiscOffer offer : offers) {
            boolean isTmSnMatched =
                (offer.getTmcode().equals(dto.getTmCode()) && offer.getSncode().equals(dto.getOfferSnCode().intValue()))
                || (offer.getTmcode() == -1 && offer.getSncode().equals(dto.getOfferSnCode().intValue()))
                || (offer.getSncode() == -1 && offer.getTmcode().equals(dto.getTmCode()))
                || (offer.getTmcode() == -1 && offer.getSncode() == -1);

            if (isTmSnMatched) {
                boolean isDateEligible =
                    (offer.getEligStartDate() == null || !assignDate.isBefore(offer.getEligStartDate())) &&
                    (offer.getEligEndDate() == null || !assignDate.isAfter(offer.getEligEndDate()));

                if (isDateEligible) {
                    return true;
                }
            }
        }

        String errMsg = String.format(
            "! AssignId %s: TM/SN (%s/%s) with AssignDate (%s) not eligible for DiscId %s.",
            dto.getAssignId(), dto.getTmCode(), dto.getOfferSnCode(), assignDate.format(formatter), dto.getDiscId());
        log.warn(errMsg);
        error.append(errMsg);
        return false;
    }

    /**
     * Checks if the offer status (e.g., Suspended, On Hold) permits the discount application.
     */
    private boolean isOfferStatusPermitted(DynDiscAssignDTO dto, StringBuilder error) {
        DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());
        if ("S".equals(String.valueOf(dto.getOfferStatus())) && !conf.getSuspInd()) {
            String errMsg = String.format(
                "! AssignId %s: Discount not allowed - Offer Suspended. SNCode: %s",
                dto.getAssignId(), dto.getOfferSnCode());
            log.info(errMsg);
            error.append(errMsg);
            return false;
        }

        if ("O".equals(String.valueOf(dto.getOfferStatus()))) {
            String errMsg = String.format(
                "! AssignId %s: Discount not allowed - Offer On Hold. SNCode: %s",
                dto.getAssignId(), dto.getOfferSnCode());
            log.info(errMsg);
            error.append(errMsg);
            return false;
        }
        return true;
    }
}
