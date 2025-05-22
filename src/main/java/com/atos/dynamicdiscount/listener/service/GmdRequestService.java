package com.atos.dynamicdiscount.listener.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atos.dynamicdiscount.model.dto.GmdRequestHistoryDTO;
import com.atos.dynamicdiscount.repository.DynDiscProcessRepository;
import com.atos.dynamicdiscount.repository.GmdRequestHistoryRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GmdRequestService {

	@Autowired
	private DynDiscProcessRepository dynDiscProcessRepository;

	@Autowired
	private GmdActionHandler requestActionHandler;

	@Autowired
	private GmdRequestHistoryRepository gmdRequestHistoryRepository;

	/**
	 * Main method to start the job processing.
	 */
	public void processJob() {
		long processId = ProcessHandle.current().pid(); // Get the current Linux process ID
		log.info("Starting job processing with process ID: {}", processId);

		boolean jobInitialized = false; 
		try {
			if (!initializeJob(processId)) {
				log.error("Job processing has been aborted.");
				return;
			}
			jobInitialized = true; 

			Long lastProcessedRequestByListener = dynDiscProcessRepository.findLastProcessedRequest(processId);
			if (lastProcessedRequestByListener == null) {
				log.error(" Last processed request by Dynamic Discount listener could not be found.");
				log.error(" Job processing has been aborted.");
				return;
			}

			//Long maxGmdRequest = gmdRequestHistoryRepository.findMaxRequest();
			 Long maxGmdRequest = 296562677l;
			if (maxGmdRequest == null) {
				log.error(" Last processed request by GMD could not be found. Aborting job processing.");
				log.error(" Job processing has been aborted.");
				return;
			}

			processRequests(lastProcessedRequestByListener, maxGmdRequest);
			dynDiscProcessRepository.updateLastProcessedRequest(maxGmdRequest, processId);
			log.info("Job processing completed for process ID: {}", processId);
		} catch (Exception e) {
			log.error("Error during job processing: ", e);
		} finally {

			if (jobInitialized) { // Clear process ID only if job was initialized
				clearProcessId(processId);
			}
			log.info("-----------------------------------------------------------");
		}
	}

	/**
	 * Initializes the job by checking for existing processes and updating the
	 * process ID in the database.
	 */
	private boolean initializeJob(long processId) {
		log.info(" Initializing job with process ID: {}", processId);

		try {
			// Step 1: Check if there's an existing process. Use Optional for a more robust
			// way to handle
			Optional<Long> existingProcessId = dynDiscProcessRepository.findActiveProcessId();

			if (existingProcessId.isPresent()) {
				log.warn(" Initialization aborted: Another process (ID: {}) is already running or crashed.",
						existingProcessId.get());
				return false;
			}

			// Step 2: Update the process ID
			int rowsUpdated = dynDiscProcessRepository.updateProcessIdForNewJob(processId);
			if (rowsUpdated != 1) {
				log.error(" Failed to update process ID. Rows updated: {}. Check database and application state.",
						rowsUpdated);
				return false;
			}
			log.info(" Job Initializing completed for process ID: {}", processId);
			return true;

		} catch (Exception e) {
			log.error(" Error initializing job with process ID {}: {}", processId, e.getMessage());
			return false;
		}
	}

	/**
	 * Process requests within the specified range.
	 */
	private void processRequests(Long start, Long end) {
		log.info(" Processing requests from {} to {}.", start, end);
		try {
			List<GmdRequestHistoryDTO> requests = gmdRequestHistoryRepository.findRequestsInRange(start, end);
			log.info(" Fetched {} requests for processing.", requests.size());

			for (GmdRequestHistoryDTO request : requests) {
				try {
					requestActionHandler.handleAction(request);
					log.debug(" Successfully processed request ID: {}", request.getRequest());
				} catch (Exception e) {
					log.error(" Error processing request ID: {}. Error: {}", request.getRequest(), e.getMessage());
				    throw e; 
				}
			}
			log.info(" Requests processing completed.");

		} catch (Exception e) {
			log.error(" Error fetching or processing requests: ", e);
			throw e; 
		}
	}

	/**
	 * Clear the process ID in the database.
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
