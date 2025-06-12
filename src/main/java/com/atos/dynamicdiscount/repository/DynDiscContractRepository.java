package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscContractId;

@Repository
public interface DynDiscContractRepository extends JpaRepository<DynDiscContract, DynDiscContractId> {

	@Modifying
	@Query(nativeQuery = true, value = """
		    INSERT INTO dyn_disc_contract_temp
			WITH
			  /*--------------------------------------------
			   1) Fetch Valid Discount Assignments
			      - Retrieve active assignments for the target date
			      - Join with customer and contract details
			      - Pull lbc_date, prgcode, tmcode
			  --------------------------------------------*/
			  valid_assigns AS (
			    SELECT
			      /*+ parallel (d,4) */
			      d.customer_id,
			      d.co_id,
			      cu.lbc_date,
			      cu.prgcode,
			      ca.tmcode
			    FROM dyn_disc_assign d
			    JOIN customer_all cu
			      ON cu.customer_id = d.customer_id
			    JOIN contract_all ca
			      ON ca.customer_id = cu.customer_id
			    WHERE
			      d.assign_date < :targetDate
			      --AND ca.co_id in (34230626)
			      AND (d.delete_date IS NULL OR d.delete_date >= :targetDate)
			      AND (d.expire_date IS NULL OR d.expire_date >= :targetDate)
			      AND (d.last_applied_date is NULL or d.last_applied_date < :targetDate)

			  ),

			  /*--------------------------------------------
			   2) Fetch Latest Bill Cycle
			      - Join with bill cycle assignment history
			      - Pick the most recent row per co_id
			  --------------------------------------------*/
			  bc_ranked AS (
			    SELECT
			    /*+ parallel (b,4) */
			      va.*,
			      b.billcycle,
			      ROW_NUMBER() OVER (
			        PARTITION BY va.co_id
			        ORDER BY
			          b.valid_from DESC,
			          b.seqno DESC
			      ) AS rn_bc
			    FROM valid_assigns va
			    JOIN billcycle_assignment_history b
			      ON b.customer_id = va.customer_id
			    WHERE
			      b.valid_from <=:targetDate
			  ),

			  /*--------------------------------------------
			   3) Filter Assignments by Target Bill Cycle
			      - Only include assignments matching the target bill cycle
			  --------------------------------------------*/
			  valid_assign_with_billcycle AS (
			    SELECT
			      br.*
			    FROM bc_ranked br
			    WHERE
			      rn_bc = 1
			      AND billcycle = :targetBillcycle
			  )

			/*--------------------------------------------
			  Final Result
			  - Retrieve required fields 
			--------------------------------------------*/
			SELECT
			  customer_id,
			  co_id,
			  lbc_date,
			  prgcode,
			  tmcode
			FROM
			  valid_assign_with_billcycle
					""")
	int persistEligibleContractsToTempTable(@Param("targetDate") LocalDateTime cutoffDate,
			@Param("targetBillcycle") String targetBillcycle);
	
	
	

	@Query(value = """
			SELECT COUNT(*)
			  FROM dyn_disc_contract
			 WHERE request_id = :requestId
			   AND status     = :status
			""", nativeQuery = true)
	long countByReqIdAndStatus(@Param("requestId") Integer requestId, @Param("status") String status);

	@Modifying
	@Query(value = """
			  UPDATE dyn_disc_contract
			     SET status = 'I'
			   WHERE request_id = :requestId
			     AND status  in ('I', 'F')
			""", nativeQuery = true)
	int resetFailedContracts(@Param("requestId") Integer requestId);

	@Query(value = """
			  SELECT *
			    FROM dyn_disc_contract
			   WHERE request_id = :requestId
			     AND status = :status
			""", nativeQuery = true)
	List<DynDiscContract> getContractsByStatus(@Param("requestId") Integer requestId, @Param("status") String status);

	
    // Insert contracts from the temp table to the final table
    @Modifying
    @Query(value = "INSERT INTO dyn_disc_contract (request_id, customer_id, co_id, lbc_date, prgcode, tmcode, status) " +
                   "SELECT :requestId, customer_id, co_id, lbc_date, prgcode, tmcode, 'I' FROM dyn_disc_contract_temp", 
           nativeQuery = true)
    int insertContractsFromTempTable(@Param("requestId") Integer requestId);

    // Truncate the temp table
    @Modifying
    @Query(value = "TRUNCATE TABLE dyn_disc_contract_temp", nativeQuery = true)
    void truncateTempTable();

    // Fetch contracts by request ID using native SQL
    @Query(value = """
    	    SELECT * 
    	    FROM DYN_DISC_CONTRACT 
    	    WHERE REQUEST_ID = :requestId 
    	      AND STATUS = 'I'
    	    ORDER BY CO_ID
    	""", nativeQuery = true)
    List<DynDiscContract> fetchAllUnprocessedContracts(@Param("requestId") Integer requestId);
    
    






}
