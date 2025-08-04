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
 * Repository for DYN_DISC_GMD_QUEUE table, providing custom queries for queue processing.
 */
@Repository
public interface DynDiscGmdQueueRepository extends JpaRepository<DynDiscGmdQueue, Long> {

    /**
     * Fetches all initial (STATUS = 'I') records from the queue
     * by joining with service history and discount configuration tables.
     */
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

    /**
     * Updates the status of a specific request to 'P' (Processed).
     */
    @Modifying
    @Query(value = """
        UPDATE DYN_DISC_GMD_QUEUE
        SET STATUS = 'P'
        WHERE REQUEST = :requestId
        """, nativeQuery = true)
    void updateStatusToProcessed(@Param("requestId") Long requestId);

    /**
     * Archives a processed request to the history table with status 'P' and timestamp.
     */
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

    /**
     * Deletes a processed request from the live queue table.
     */
    @Modifying
    @Query(value = """
        DELETE FROM DYN_DISC_GMD_QUEUE
        WHERE REQUEST = :requestId
        """, nativeQuery = true)
    void deleteFromQueue(@Param("requestId") Long requestId);

    /**
     * Combined operation to mark a request as processed, archive it, and then delete it.
     */
    @Modifying
    @Transactional
    default void markAsProcessedAndArchive(Long requestId) {
        updateStatusToProcessed(requestId);
        archiveProcessedRequest(requestId);
        deleteFromQueue(requestId);
    }

    /**
     * Marks a request as errored by updating its status to 'E'.
     */
    @Modifying
    @Query(value = """
        UPDATE DYN_DISC_GMD_QUEUE
        SET STATUS = 'E'
        WHERE REQUEST = :requestId
        """, nativeQuery = true)
    void markAsError(@Param("requestId") Long requestId);

    /**
     * Marks all unqualified records as skipped (STATUS = 'S').
     * Unqualified = not in eligible set OR already assigned OR has assignment state.
     */
    @Modifying
    @Query(value = """
        UPDATE DYN_DISC_GMD_QUEUE q
        SET q.STATUS = 'S'
        WHERE q.STATUS = 'I'
        AND (
            q.REQUEST NOT IN (
                SELECT q2.REQUEST
                FROM DYN_DISC_GMD_QUEUE q2
                JOIN pr_serv_status_hist psh 
                    ON q2.CO_ID = psh.CO_ID
                   AND q2.REQUEST = psh.REQUEST_ID
                JOIN dyn_disc_conf ddc 
                    ON psh.SNCODE = ddc.DISC_SNCODE
                WHERE q2.STATUS = 'I'
            )
            OR EXISTS (
                SELECT 1 FROM DYN_DISC_ASSIGN da WHERE da.GMD_REQUEST = q.REQUEST
            )
            OR EXISTS (
                SELECT 1 FROM DYN_DISC_ASSIGN_STATE das WHERE das.GMD_REQUEST = q.REQUEST
            )
        )
        """, nativeQuery = true)
    void markAllUnqualifiedAsSkipped();

    /**
     * Archives all skipped requests (STATUS = 'S') into the history table.
     */
    @Modifying
    @Query(value = """
        INSERT INTO DYN_DISC_GMD_QUEUE_HIST
        (REQUEST, INSERTED_AT, CUSTOMER_ID, CO_ID, ACTION_ID, STATUS, MOVED_AT)
        SELECT REQUEST, INSERTED_AT, CUSTOMER_ID, CO_ID, ACTION_ID, 'S', SYSDATE
        FROM DYN_DISC_GMD_QUEUE
        WHERE STATUS = 'S'
        """, nativeQuery = true)
    void archiveSkippedRequests();

    /**
     * Deletes all skipped requests (STATUS = 'S') from the live queue.
     */
    @Modifying
    @Query(value = """
        DELETE FROM DYN_DISC_GMD_QUEUE
        WHERE STATUS = 'S'
        """, nativeQuery = true)
    void deleteSkippedRequests();

    /**
     * Combined operation to mark all unqualified records as skipped,
     * archive them, and then remove them from the live queue.
     */
    @Modifying
    @Transactional
    default void skipUnqualifiedAndArchive() {
        markAllUnqualifiedAsSkipped();
        archiveSkippedRequests();
        deleteSkippedRequests();
    }
}
