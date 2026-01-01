package com.atos.dynamicdiscount.listener.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.entity.DynDiscFirstLoginTrack;
import com.atos.dynamicdiscount.repository.DynDiscFirstLoginTrackRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirstLoginTrackService {

    private final DynDiscFirstLoginTrackRepository trackRepository;
    private final FirstLoginRecordProcessor recordProcessor;

    /**
     * Encapsulated scheduled job logic
     */
    public void processJob() {
        log.info("-----------------------------------------------------------");
        log.info("Starting first login delta job.");

        insertDelta();
        processPendingFirstLogins();

        log.info("Completed first login delta job.");
        log.info("-----------------------------------------------------------");

    }

    /**
     * Insert delta from source table
     */
    public int insertDelta() {
        int count = trackRepository.insertDeltaFromSource();
        log.info("Inserted {} new records into DynDiscFirstLoginTrack.", count);
        return count;
    }

    /**
     * Retrieve unprocessed rows
     */
    public List<DynDiscFirstLoginTrack> getUnprocessedRecords() {
        return trackRepository.findByProcessedFlg("N");
    }

    /**
     * Loop over records — each processed in its own transaction
     */
    public void processPendingFirstLogins() {
        List<DynDiscFirstLoginTrack> unprocessed = getUnprocessedRecords();
        log.info("Processing {} unprocessed first login records.", unprocessed.size());

        for (DynDiscFirstLoginTrack record : unprocessed) {
            try {
                recordProcessor.processSingleRecord(record);
            } catch (Exception ex) {
                log.error("❌ Error processing CO_ID={}", record.getCoId(), ex);
                // continue with next record
            }
        }
    }
}
