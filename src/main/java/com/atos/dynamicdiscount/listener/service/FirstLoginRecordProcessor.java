package com.atos.dynamicdiscount.listener.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.atos.dynamicdiscount.model.entity.DynDiscFirstLoginTrack;
import com.atos.dynamicdiscount.repository.DynDiscFirstLoginTrackRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirstLoginRecordProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final DynDiscFirstLoginTrackRepository trackRepository;

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
     * One record = one transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleRecord(DynDiscFirstLoginTrack record) {


        boolean success = true;
        StringBuilder comments = new StringBuilder();

        Integer coId = record.getCoId();
        Integer customerId = record.getCustomerId();
        Integer tmcode = record.getTmcode();
        LocalDateTime firstLogin = record.getFirstLogin();

        String[] remarks = { occRemark1, occRemark2 };
        double[] amounts = { occAmount, -occAmount };

        try {
            for (int i = 0; i < remarks.length; i++) {
                callOccProcedure(customerId, coId, tmcode, firstLogin, remarks[i], amounts[i]);
            }

            comments.append("OCCs granted successful.");

        } catch (Exception ex) {
            success = false;
            comments.append("OCC failed: ").append(ex.getMessage());
            log.error("✗ CO_ID={} OCC processing failed", coId, ex);

            // force rollback
            throw ex;
        } finally {
            trackRepository.updateProcessingStatus(
                    coId,
                    success ? "Y" : "F",
                    LocalDateTime.now(),
                    comments.toString()
            );
            
			log.info("Processing completed for CO_ID={} with status={}.", coId, success ? "SUCCESS" : "FAILED");
        }
    }

    /**
     * OCC stored procedure call
     */
    private void callOccProcedure(Integer customerId,
                                  Integer contractId,
                                  Integer tmcode,
                                  LocalDateTime firstLoginDate,
                                  String remark,
                                  double amount) {

        

        if (!isGrantOccEnabled) {
            jdbcTemplate.update(
                    "CALL grant_promo_Result (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    -1,
                    customerId,
                    contractId,
                    firstLoginDate,
                    amount,
                    remark,
                    occGlcode,
                    occSncode,
                    tmcode,
                    firstLoginDate
            );
        } else {
            jdbcTemplate.update(
                    "CALL bscs_wd.mcd_wan_pkg.man_addocc (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    customerId,
                    contractId,
                    firstLoginDate,
                    amount,
                    remark,
                    occGlcode,
                    occSncode,
                    tmcode,
                    firstLoginDate
            );
        }

    }
}
