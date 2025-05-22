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
     * @param billCycle the bill cycle code to validate
     * @throws InvalidBillCycleException if the code is unrecognized
     */
    public void validateBillCycle(String billCycle) {
        try {
            BillCycle.fromString(billCycle);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid bill cycle [{}]", billCycle);
            throw new InvalidBillCycleException("Invalid bill cycle: " + billCycle, ex);
        }
    }

    /**
     * Retrieves the cutoff date for the given bill cycle.
     * @param billCycle the bill cycle code
     * @return the cutoff date (never in the future)
     * @throws InvalidBillCycleException if no date is found or it is in the future
     */
    public LocalDateTime fetchCutoffDate(String billCycle) {
        LocalDateTime cutoffDate = billCycleDefinitionRepository
                .findTargetRunDateByBillCycle(billCycle);

        if (cutoffDate == null) {
            log.error("No cutoff date found for bill cycle [{}]", billCycle);
            throw new InvalidBillCycleException(
                "No cutoff date for bill cycle: " + billCycle
            );
        }

        if (cutoffDate.isAfter(LocalDateTime.now())) {
            log.error(
                "Cutoff date [{}] for bill cycle [{}] is in the future", cutoffDate, billCycle
            );
            throw new InvalidBillCycleException(
                "Cutoff date for bill cycle " + billCycle
                + " is in the future: " + cutoffDate
            );
        }

        log.info("Cutoff date for [{}] is {}", billCycle, cutoffDate);
        return cutoffDate;
    }
}
