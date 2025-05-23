
package com.atos.dynamicdiscount.processor.service.evaluation;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscConf;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonthId;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscOffer;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonthId;
import com.atos.dynamicdiscount.processor.config.DynDiscConfigurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCalculator {

    private final DynDiscConfigurations cfg;

    /**
     * Extracted all discount-amount computation (free/special/capping) logic.
     */
    public DynDiscGrantHistory compute(DynDiscContract contract, DynDiscAssignDTO dto) {
        long assignId = dto.getAssignId().longValue();
        log.debug("✓ AssignId {}: Starting discount computation...", assignId);

        DynDiscOffer offer = findOffer(dto)
            .orElseThrow(() -> new IllegalStateException(
                "Offer missing for DiscId=" + dto.getDiscId()));

        DynDiscConf conf = cfg.getDynDiscConfMap().get(dto.getDiscId().intValue());
        int monthNo = Optional.ofNullable(dto.getApplyCount().intValue()).map(Integer::intValue).orElse(0) + 1;
        boolean aloInd = conf.getAloDiscInd();
        float baseOffer = dto.getOfferPrice().floatValue();
        float baseAlo   = dto.getAloPrice().floatValue();

        // Determine discount amounts
        boolean isFree    = offer.getFreeMonthInd()
                             && cfg.getDynDiscFreeMonthMap()
                                   .containsKey(new DynDiscFreeMonthId(offer.getOfferId(), monthNo));
        boolean isSpecial = offer.getSpecialMonthInd()
                             && cfg.getDynDiscSpecialMonthMap()
                                   .containsKey(new DynDiscSpecialMonthId(offer.getOfferId(), monthNo));

        float discAmt = 0f, aloAmt = 0f;
        if (isFree) {
            log.debug("Free month: AssignId {}", assignId);
            discAmt = baseOffer;
            aloAmt  = aloInd ? baseAlo : 0f;
        } else if (isSpecial) {
            log.debug("Special month: AssignId {}", assignId);
            discAmt = cfg.getDynDiscSpecialMonthMap()
                         .get(new DynDiscSpecialMonthId(offer.getOfferId(), monthNo))
                         .getOfferDiscAmt();
            aloAmt  = aloInd 
                     ? cfg.getDynDiscSpecialMonthMap()
                          .get(new DynDiscSpecialMonthId(offer.getOfferId(), monthNo))
                          .getAloDiscAmt()
                     : 0f;
        } else {
            discAmt = offer.getOfferDiscAmt();
            aloAmt  = aloInd ? offer.getAloDiscAmt(): 0f;
        }

        boolean offerCapped = capAndLog("Offer price", discAmt, baseOffer);
        if (offerCapped) discAmt = baseOffer;

        boolean aloCapped = aloInd && capAndLog("ALO price", aloAmt, baseAlo);
        if (aloCapped) aloAmt = baseAlo;

        boolean isLast = conf.getDuration() != null 
                         && conf.getDuration() != -1 
                         && monthNo == conf.getDuration();

        log.info("✓ AssignId {}: Discount Computation completed.", assignId);

        return DynDiscGrantHistory.builder()
            .requestId(contract.getRequestId())
            .assignId(assignId)
            .currentApplyCount(monthNo)
            .lastApply(isLast)
            .offerDiscAmount(discAmt)
            .aloDiscAmount(aloAmt)
            .freeMonth(isFree)
            .specialMonth(isSpecial)
            .offerCapped(offerCapped)
            .aloCapped(aloCapped)
            .aloDiscInd(aloInd)
            .note("Successfully applied")
            .aloOccCreated(false)
            .offerOccCreated(false)
            .build();
    }


    
    private Optional<DynDiscOffer> findOffer(DynDiscAssignDTO dto) {
        List<DynDiscOffer> offers = cfg.getDynDiscOfferMap()
            .getOrDefault(dto.getDiscId().intValue(), List.of());

        DynDiscOffer bestMatch = null;
        int bestPriority = Integer.MAX_VALUE;

        for (DynDiscOffer o : offers) {
            int priority;
            if (o.getTmcode().equals(dto.getTmCode()) && o.getSncode().equals(dto.getOfferSnCode().intValue())) {
                priority = 1; // Exact match
            } else if (o.getTmcode() == -1 && o.getSncode().equals(dto.getOfferSnCode().intValue())) {
                priority = 2; // SNCode match with TMCode = -1
            } else if (o.getSncode() == -1 && o.getTmcode().equals(dto.getTmCode())) {
                priority = 3; // TMCode match with SNCode = -1
            } else if (o.getTmcode() == -1 && o.getSncode() == -1) {
                priority = 4; // Catch-all
            } else {
                continue; // Skip irrelevant offers
            }

            if (priority < bestPriority) {
                bestPriority = priority;
                bestMatch = o;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    private boolean capAndLog(String label, float value, float max) {
        if (value > max) {
            log.debug("{} {} exceeds {}; capping to {}", label, value, max, max);
            return true;
        }
        return false;
    }

    private float firstNonZero(Float primary, Float fallback) {
        return (primary != null && primary != 0f) ? primary : fallback;
    }
}
