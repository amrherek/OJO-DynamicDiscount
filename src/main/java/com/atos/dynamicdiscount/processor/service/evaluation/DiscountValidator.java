package com.atos.dynamicdiscount.processor.service.evaluation;

import static java.util.Collections.emptyList;

import java.time.LocalDateTime;
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
    public boolean isValid(DynDiscAssignDTO dto, LocalDateTime cutoffDate) {
        if (!hasValidConfig(dto, cutoffDate)) {
            return false;
        }

        if (isPriceGroupExcluded(dto)) {
            return false;
        }

        if (!isOfferEligible(dto)) {
            return false;
        }

        if (!isOfferStatusPermitted(dto)) {
            return false;
        }

        log.info("✓ AssignId {}: Valid discount.", dto.getAssignId());
        return true;
    }

    /**
     * Checks if the discount configuration exists, is active, and its application limit has not been reached.
     */
    private boolean hasValidConfig(DynDiscAssignDTO dto, LocalDateTime cutoffDate) {
        DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());
        if (conf == null) {
            log.warn("! AssignId {}: No config for DiscId {}.", dto.getAssignId(), dto.getDiscId());
            return false;
        }

        if (conf.getValidTo() != null && !conf.getValidTo().isAfter(cutoffDate)) {
            log.info("! AssignId {}: Expired on {}.", dto.getAssignId(), conf.getValidTo());
            return false;
        }
        
        
        
        int applied = dto.getApplyCount() != null ? dto.getApplyCount().intValue() : 0;
        int limit = (dto.getOvwApplyCount() != null) ? dto.getOvwApplyCount().intValue() : (conf.getDuration() != null ? conf.getDuration() : -1);
        if (limit != -1 && applied >= limit) {
            log.info("! AssignId {}: Limit reached.", dto.getAssignId());
            return false;
        }
        
                

        if (conf.getDuration() != null && conf.getDuration() != -1 && applied >= conf.getDuration()) {
            log.info("! AssignId {}: Limit reached.", dto.getAssignId());
            return false;
        }

        return true;
    }

    /**
     * Checks if the customer's price group is excluded or restricted from the discount.
     */
    private boolean isPriceGroupExcluded(DynDiscAssignDTO dto) {

        // advanced price group rules
        List<DynDiscPriceGroup> groups = cfg.getDynDiscPriceGroupMap().values().stream()
            .filter(pg -> pg.getId().getDiscId().equals(dto.getDiscId().intValue()))
            .filter(pg -> pg.isRestrictInd() != pg.isProhibitInd()) // Filter out invalid entries
            .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return false; // No specific price group rules apply, so not excluded by this logic
        }

        Set<String> restricted = groups.stream()
            .filter(DynDiscPriceGroup::isRestrictInd)
            .map(pg -> pg.getId().getPrgcode())
            .collect(Collectors.toSet());

        if (!restricted.isEmpty() && !restricted.contains(dto.getPrgcode())) {
            log.info("! AssignId {}: Discount restricted - Customer price group '{}' not allowed.",
                     dto.getAssignId(), dto.getPrgcode());
            return true; // Excluded by restriction
        }

        Set<String> prohibited = groups.stream()
            .filter(DynDiscPriceGroup::isProhibitInd)
            .map(pg -> pg.getId().getPrgcode())
            .collect(Collectors.toSet());

        if (!prohibited.isEmpty() && prohibited.contains(dto.getPrgcode())) {
            log.info("! AssignId {}: Discount prohibited - Customer price group '{}' is excluded.",
                     dto.getAssignId(), dto.getPrgcode());
            return true; // Excluded by prohibition
        }

        return false; // Not explicitly excluded by price group rules
    }

    /**
     * Checks if the discount offer is eligible based on TMCode and SNCode matching.
     */
    private boolean isOfferEligible(DynDiscAssignDTO dto) {
        List<DynDiscOffer> offers = cfg.getDynDiscOfferMap()
            .getOrDefault(dto.getDiscId().intValue(), emptyList());

        boolean match = offers.stream().anyMatch(o ->
            (o.getTmcode().equals(dto.getTmCode()) && o.getSncode().equals(dto.getOfferSnCode().intValue())) || // Exact match
            (o.getTmcode() == -1 && o.getSncode().equals(dto.getOfferSnCode().intValue())) || // SNCode match with TMCode = -1
            (o.getSncode() == -1 && o.getTmcode().equals(dto.getTmCode())) || // TMCode match with SNCode = -1
            (o.getTmcode() == -1 && o.getSncode() == -1) // Global catch-all
        );

        if (!match) {
            log.warn("! AssignId {}: TM/SN ({}/{}) not eligible for DiscId {}.", dto.getAssignId(),
                     dto.getTmCode(), dto.getOfferSnCode(), dto.getDiscId());
        }

        return match;
    }

    /**
     * Checks if the offer status (e.g., Suspended, On Hold) permits the discount application.
     */
    private boolean isOfferStatusPermitted(DynDiscAssignDTO dto) {
        DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());

        if ("S".equals(String.valueOf(dto.getOfferStatus())) && !conf.getSuspInd()) {
            log.info("! AssignId {}: Discount not allowed - Offer Suspended. SNCode: {}", dto.getAssignId(), dto.getOfferSnCode());
            return false;
        }

        if ("O".equals(String.valueOf(dto.getOfferStatus()))) {
            log.info("! AssignId {}: Discount not allowed - Offer On Hold. SNCode: {}", dto.getAssignId(), dto.getOfferSnCode());
            return false;
        }

        return true;
    }

}