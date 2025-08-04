package com.atos.dynamicdiscount.listener.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.DynDiscGmdQueueDTO;
import com.atos.dynamicdiscount.repository.DynDiscGmdQueueRepository;
import com.atos.dynamicdiscount.repository.DynDiscProcessRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GmdRequestService {

    @Autowired
    private DynDiscProcessRepository dynDiscProcessRepository;

    @Autowired
    private GmdActionHandler requestActionHandler;

    @Autowired
    private DynDiscGmdQueueRepository dynDiscGmdQueueRepository;

    /**
     * Main entry point for processing Dynamic Discount GMD requests.
     * It performs:
     *  - Initialization and process registration
     *  - Fetching pending ('I') requests
     *  - Handling each request individually
     *  - Updating status and archiving successfully handled requests
     *  - Marking failed requests with error status
     */
    public void processJob() {
        long processId = ProcessHandle.current().pid();
        log.info("Starting Dynamic Discount job with process ID: {}", processId);

        boolean jobInitialized = false;

        try {
            // Step 0: Initialization and PID registration
            if (!initializeJob(processId)) {
                log.error("Job initialization failed. Aborting.");
                return;
            }
            jobInitialized = true;
            
            
            // Step 0: Immediately skip everything unqualified ───
            dynDiscGmdQueueRepository.skipUnqualifiedAndArchive();
            log.info("Skipped & archived all currently unqualified 'I' requests.");

            // Step 1: Fetch 'I' (initial) status requests from the processing queue
            List<DynDiscGmdQueueDTO> initialRequests = dynDiscGmdQueueRepository.fetchInitialRequests();

            if (initialRequests == null || initialRequests.isEmpty()) {
                log.info("No pending requests found in the queue. Nothing to process.");
                return;
            }

            log.info("Fetched {} pending requests for processing.", initialRequests.size());

            // Step 2: Process requests one by one
            for (DynDiscGmdQueueDTO request : initialRequests) {
                try {
                    requestActionHandler.handleAction(request);

                    // Step 2a: On success — mark as 'P' and move to archive table
                    dynDiscGmdQueueRepository.markAsProcessedAndArchive(request.getRequest().longValue());
                    log.debug("Successfully processed request ID: {}", request.getRequest());

                } catch (Exception e) {
                    // Step 2b: On failure — mark as 'E' (error)
                    dynDiscGmdQueueRepository.markAsError(request.getRequest().longValue());
                    log.error("Error processing request ID {}: {}", request.getRequest(), e.getMessage());
                }
            }

            log.info("Dynamic Discount job completed successfully for process ID: {}", processId);

        } catch (Exception e) {
            log.error("Unexpected error during Dynamic Discount job execution: ", e);
        } finally {
            // Step 3: Cleanup
            if (jobInitialized) {
                clearProcessId(processId);
            }
            log.info("-----------------------------------------------------------");
        }
    }

    /**
     * Registers the current job run by checking for active processes,
     * then updating the process ID in the database.
     *
     * @param processId the current system process ID
     * @return true if initialization succeeds, false otherwise
     */
    private boolean initializeJob(long processId) {
        log.info("Initializing job with process ID: {}", processId);
        try {
            // Check if another process is already registered
            Optional<Long> existingProcessId = dynDiscProcessRepository.findActiveProcessId();

            if (existingProcessId.isPresent()) {
                log.warn("Another process (ID: {}) is already active. Aborting current job.",
                        existingProcessId.get());
                return false;
            }

            // Register current process
            int rowsUpdated = dynDiscProcessRepository.updateProcessIdForNewJob(processId);
            if (rowsUpdated != 1) {
                log.error("Failed to register process ID. Expected 1 row to be updated, but got: {}", rowsUpdated);
                return false;
            }

            log.info("Job initialization successful for process ID: {}", processId);
            return true;

        } catch (Exception e) {
            log.error("Error initializing job with process ID {}: {}", processId, e.getMessage());
            return false;
        }
    }

    /**
     * Clears the registered process ID after job completion or failure.
     *
     * @param processId the process ID to be cleared
     */
    private void clearProcessId(long processId) {
        log.info("Clearing process ID: {}", processId);
        try {
            dynDiscProcessRepository.clearProcessId(processId);
            log.info("Successfully cleared process ID: {}", processId);
        } catch (Exception e) {
            log.error("Error while clearing process ID: {}", processId, e);
        }
    }
}
