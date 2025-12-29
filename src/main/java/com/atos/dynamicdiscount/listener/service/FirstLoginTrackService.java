package com.atos.dynamicdiscount.listener.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atos.dynamicdiscount.model.entity.DynDiscFirstLoginTrack;
import com.atos.dynamicdiscount.repository.DynDiscFirstLoginTrackRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirstLoginTrackService {

    private final DynDiscFirstLoginTrackRepository trackRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${firstlogin.occ.remark1}")
    private String occRemark1;

    @Value("${firstlogin.occ.remark2}")
    private String occRemark2;

    @Value("${firstlogin.occ.amount}")
    private double occAmount;

    @Value("${firstlogin.occ.sncode}")
    private Integer occSncode;

    @Value("${firstlogin.occ.glcode}")
    private String occGlcode;
    
    @Value("${occ.grant.enabled}")
    private boolean isGrantOccEnabled;

    /**
     * Insert delta from source table
     */
    @Transactional
    public int insertDelta() {
        try {
            int count = trackRepository.insertDeltaFromSource();
            log.info("Inserted {} new records into DynDiscFirstLoginTrack.", count);
            return count;
        } catch (Exception e) {
            log.error("Error inserting delta: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieve unprocessed rows
     */
    @Transactional(readOnly = true)
    public List<DynDiscFirstLoginTrack> getUnprocessedRecords() {
        return trackRepository.findByProcessedFlg("N");
    }

    /**
     * Update processing status, processing date and comments
     */
    @Transactional
    public void updateProcessingStatus(Integer coId, String processedFlg, LocalDateTime processedDate, String comments) {
        int updated = trackRepository.updateProcessingStatus(coId, processedFlg, processedDate, comments);

        if (updated > 0) {
            log.info("CO_ID {} marked as {}", coId, processedFlg.equals("Y") ? "processed" : "failed");
        } else {
            log.warn("No record found for CO_ID {} to update status", coId);
        }
    }

    /**
     * Call stored procedure for OCC grant
     */
	private void callOccProcedure(Integer customerId, Integer contractId, LocalDateTime firstLoginDate, String remark,
			double amount, DynDiscFirstLoginTrack record, Integer tmcode) {

		if (!isGrantOccEnabled) {
			jdbcTemplate.update("CALL grant_promo_Result (?, ?, ?, ?, ?, ?, ?, ?, ?,?)", -1, customerId, contractId,
					firstLoginDate, amount, remark, occGlcode, occSncode, tmcode, firstLoginDate);
		}

		else {

			jdbcTemplate.update("CALL bscs_wd.mcd_wan_pkg.man_addocc(?, ?, ?, ?, ?, ?, ?, ?, ?)", customerId,
					contractId, firstLoginDate, amount, remark, occGlcode, occSncode, tmcode, firstLoginDate);
		}
		log.debug("✓ coId={} : {} OCC grant completed with amount {}", contractId, remark, amount);
	}

    /**
     * Process all unprocessed rows, call stored procedures, handle errors
     */
    @Transactional
    public void processPendingFirstLogins() {
        List<DynDiscFirstLoginTrack> unprocessed = getUnprocessedRecords();
        log.info("Processing {} unprocessed first login records.", unprocessed.size());

        for (DynDiscFirstLoginTrack record : unprocessed) {
            boolean success = true;
            StringBuilder comments = new StringBuilder();
            long startTimeTotal = System.currentTimeMillis();

            Integer coId = record.getCoId();
            Integer customerId = record.getCustomerId();
            Integer tmcode = record.getTmcode();
            LocalDateTime validFrom = record.getFirstLogin();

            // Two OCCs per record: first positive, second negative
            String[] occRemarks = {occRemark1, occRemark2};
            double[] occAmounts = {occAmount, -occAmount};

            for (int i = 0; i < occRemarks.length; i++) {
                String remark = occRemarks[i];
                double amount = occAmounts[i];

                try {
                    callOccProcedure(customerId, coId, validFrom, remark, amount, record, tmcode);
                } catch (Exception ex) {
                    success = false;
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    comments.append(remark).append(" failed: ").append(errorMsg).append("; ");
                    log.error("✗ coId={} : {} OCC grant failed. {}", coId, remark, errorMsg, ex);
                }
            }

            if (success) {
                comments.append("All OCC grants successful.");
            }

            // Update row status
            try {
                updateProcessingStatus(coId, success ? "Y" : "F", LocalDateTime.now(), comments.toString());
            } catch (Exception e) {
                log.error("Error updating processing status for CO_ID {}: {}", coId, e.getMessage(), e);
            }

            long durationTotal = System.currentTimeMillis() - startTimeTotal;
            log.debug("coId={} : Total processing time = {} ms", coId, durationTotal);
        }
    }

    /**
     * Encapsulated scheduled job logic
     */
    @Transactional
    public void processJob() {
        log.info("Starting first login delta job.");

        // Step 1: Insert delta
        insertDelta();

        // Step 2: Process all unprocessed rows
        processPendingFirstLogins();

        log.info("Completed first login delta job.");
    }
}
