package com.atos.dynamicdiscount.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscProcess;

import jakarta.transaction.Transactional;

@Repository
public interface DynDiscProcessRepository extends JpaRepository<DynDiscProcess, Long> {

	// Query to find if any running instance of the dynamic discount
	@Query(value = "SELECT PROCESS_ID FROM DYN_DISC_PROCESS WHERE COMPONENT = 'dyn_disc'", nativeQuery = true)
	Optional<Long> findActiveProcessId();

	
	// Query to update the process ID for new job
	@Modifying
	@Transactional
	@Query(value = "UPDATE DYN_DISC_PROCESS SET PROCESS_ID = :processId ,UPDATED_AT =SYSDATE WHERE PROCESS_ID IS NULL AND COMPONENT = 'dyn_disc'", nativeQuery = true)
	int updateProcessIdForNewJob(@Param("processId") Long processId);

	// Query to find the last processed request for a specific process ID
	@Query(value = "SELECT LAST_REQ_ID FROM DYN_DISC_PROCESS WHERE PROCESS_ID = :processId AND COMPONENT = 'dyn_disc' ", nativeQuery = true)
	Long findLastProcessedRequest(@Param("processId") Long processId);

	
	// Query to update the last processed request
	@Modifying
	@Transactional
	@Query(value = "UPDATE DYN_DISC_PROCESS SET LAST_REQ_ID = :lastRequest ,UPDATED_AT =SYSDATE  WHERE PROCESS_ID = :processId AND COMPONENT = 'dyn_disc'", nativeQuery = true)
	int updateLastProcessedRequest(@Param("lastRequest") Long lastRequest, @Param("processId") Long processId);

	// Query to clear PROCESS_ID to NULL after job completion
	@Modifying
	@Transactional
	@Query(value = "UPDATE DYN_DISC_PROCESS SET PROCESS_ID = NULL ,UPDATED_AT =SYSDATE  WHERE PROCESS_ID = :processId AND COMPONENT = 'dyn_disc'", nativeQuery = true)
	void clearProcessId(@Param("processId") Long processId);

	
}
