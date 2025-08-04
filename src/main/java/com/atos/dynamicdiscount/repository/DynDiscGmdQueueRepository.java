package com.atos.dynamicdiscount.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.DynDiscGmdQueueDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscGmdQueue;

import jakarta.transaction.Transactional;

/**
 * Repository for DYN_DISC_GMD_QUEUE table.
 */
@Repository
public interface DynDiscGmdQueueRepository extends JpaRepository<DynDiscGmdQueue, Long> {

	@Query(value = """
			    SELECT
			        q.REQUEST AS request,
			        q.CUSTOMER_ID AS customerId,
			        q.CO_ID AS coId,
			        q.ACTION_ID AS actionId,
			        ddc.DISC_ID AS discId,
			        ddc.DISC_SNCODE AS discSncode,
			        psh.VALID_FROM_DATE AS validFromDate
			    FROM DYN_DISC_GMD_QUEUE q
			    JOIN pr_serv_status_hist psh
			        ON q.CO_ID = psh.CO_ID AND q.REQUEST = psh.REQUEST_ID
			    JOIN dyn_disc_conf ddc
			        ON psh.SNCODE = ddc.DISC_SNCODE
			    WHERE q.STATUS = 'I'
			    ORDER BY q.REQUEST
			""", nativeQuery = true)
	List<DynDiscGmdQueueDTO> fetchInitialRequests();

	@Modifying
	@Query(value = """
			    UPDATE DYN_DISC_GMD_QUEUE
			    SET STATUS = 'P'
			    WHERE REQUEST = :requestId
			""", nativeQuery = true)
	void updateStatusToProcessed(@Param("requestId") Long requestId);

	@Modifying
	@Query(value = """
			    INSERT INTO DYN_DISC_GMD_QUEUE_HIST (
			        REQUEST, INSERTED_AT, CUSTOMER_ID, CO_ID, ACTION_ID, STATUS, MOVED_AT
			    )
			    SELECT REQUEST, INSERTED_AT, CUSTOMER_ID, CO_ID, ACTION_ID, 'P', SYSDATE
			    FROM DYN_DISC_GMD_QUEUE
			    WHERE REQUEST = :requestId
			""", nativeQuery = true)
	void archiveProcessedRequest(@Param("requestId") Long requestId);

	@Modifying
	@Query(value = """
			    DELETE FROM DYN_DISC_GMD_QUEUE
			    WHERE REQUEST = :requestId
			""", nativeQuery = true)
	void deleteFromQueue(@Param("requestId") Long requestId);

	@Modifying
	@Transactional
	default void markAsProcessedAndArchive(Long requestId) {
		updateStatusToProcessed(requestId);
		archiveProcessedRequest(requestId);
		deleteFromQueue(requestId);
	}

	@Modifying
	@Query(value = """
			    UPDATE DYN_DISC_GMD_QUEUE
			    SET STATUS = 'E'
			    WHERE REQUEST = :requestId
			""", nativeQuery = true)
	void markAsError(@Param("requestId") Long requestId);

}
