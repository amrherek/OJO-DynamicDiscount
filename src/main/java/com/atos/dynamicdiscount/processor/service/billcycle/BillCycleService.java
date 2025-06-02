package com.atos.dynamicdiscount.processor.service.billcycle;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.enums.BillCycle;
import com.atos.dynamicdiscount.exceptions.InvalidBillCycleException;
import com.atos.dynamicdiscount.repository.BillCycleDefinitionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to validate bill cycles and fetch their cutoff dates.
 */
@Service
@Slf4j
public class BillCycleService {

    @Autowired
    private BillCycleDefinitionRepository billCycleDefinitionRepository;

    /**
     * Validates that the provided bill cycle string matches a known cycle.
     */
    
    
    public BillCycle validateBillCycle(String billCycle) {
        try {
            return BillCycle.fromString(billCycle);
        } catch (IllegalArgumentException ex) {
            log.warn("! Invalid bill cycle [{}]", billCycle);
            return null;
        }
    }


    /**
     * Retrieves the cutoff date for the given bill cycle.
     */
    public LocalDateTime fetchCutoffDate(String billCycle) {
        LocalDateTime cutoffDate = billCycleDefinitionRepository
                .findTargetRunDateByBillCycle(billCycle);

        if (cutoffDate == null) {
            log.warn("! No cutoff date found for bill cycle [{}]", billCycle);
            return cutoffDate; 
  
        }

        if (cutoffDate.isAfter(LocalDateTime.now())) {
        	log.warn(
                "! Cutoff date [{}] for bill cycle [{}] is in the future", cutoffDate, billCycle
            );
            return null;
        }

        log.info("√ Cutoff date for [{}] is {}", billCycle, cutoffDate);
        return cutoffDate;
    }
}
