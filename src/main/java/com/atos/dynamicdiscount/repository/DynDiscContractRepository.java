package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscContractId;

import jakarta.transaction.Transactional;

@Repository
public interface DynDiscContractRepository extends JpaRepository<DynDiscContract, DynDiscContractId> {

	@Query(nativeQuery = true, value = """
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
				COUNT(*)
			FROM
			  valid_assign_with_billcycle
					""")
	int countEligibleContracts(@Param("targetDate") LocalDateTime cutoffDate,
			@Param("targetBillcycle") String targetBillcycle);
	
	
	

	
	
	@Modifying
	@Query(nativeQuery = true, value = """
	    -- Insert Contracts
	    INSERT INTO dyn_disc_contract (customer_id, co_id, lbc_date, prgcode, tmcode, request_id, pack_id)
	    WITH
	      /*--------------------------------------------
	       1) Fetch Valid Discount Assignments
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
	        JOIN customer_all cu ON cu.customer_id = d.customer_id
	        JOIN contract_all ca ON ca.customer_id = cu.customer_id
	        WHERE
	          d.assign_date < :targetDate
			  --AND ca.co_id in (34230626)
	          AND (d.delete_date IS NULL OR d.delete_date >= :targetDate)
	          AND (d.expire_date IS NULL OR d.expire_date >= :targetDate)
	          AND (d.last_applied_date IS NULL OR d.last_applied_date < :targetDate)
	      ),

	      /*--------------------------------------------
	       2) Fetch Latest Bill Cycle
	      --------------------------------------------*/
	      bc_ranked AS (
	        SELECT
			 /*+ parallel (b,4) */
	          va.*,
	          b.billcycle,
	          ROW_NUMBER() OVER (
	            PARTITION BY va.co_id
	            ORDER BY b.valid_from DESC, b.seqno DESC
	          ) AS rn_bc
	        FROM valid_assigns va
	        JOIN billcycle_assignment_history b ON b.customer_id = va.customer_id
	        WHERE b.valid_from <= :targetDate
	      ),

	      /*--------------------------------------------
	       3) Filter Assignments by Target Bill Cycle
	      --------------------------------------------*/
	      valid_assign_with_billcycle AS (
	        SELECT br.*
	        FROM bc_ranked br
	        WHERE rn_bc = 1 AND billcycle = :targetBillcycle
	      ),

	      /*--------------------------------------------
	       4) Assign Pack IDs
	      --------------------------------------------*/
	      assigned_packs AS (
	        SELECT 
	          v.customer_id,
	          v.co_id,
	          v.lbc_date,
	          v.prgcode,
	          v.tmcode,
	          :requestId AS request_id,
	          CEIL(ROW_NUMBER() OVER (ORDER BY v.tmcode,v.co_id) / :batchSize) AS pack_id
	        FROM valid_assign_with_billcycle v
	      )
	    SELECT
	      customer_id,
	      co_id,
	      lbc_date,
	      prgcode,
	      tmcode,
	      request_id,
	      pack_id
	    FROM assigned_packs
	    """)
	int insertContracts(@Param("targetDate") LocalDateTime cutoffDate,
	                    @Param("targetBillcycle") String targetBillcycle,
	                    @Param("requestId") Integer requestId,
	                    @Param("batchSize") Integer batchSize);

	
	

	
	
	 // Count contracts by request ID and status
    @Query(value = """
        SELECT COUNT(*)
        FROM dyn_disc_contract
        WHERE request_id = :requestId AND status = :status
    """, nativeQuery = true)
    long countByReqIdAndStatus(Integer requestId, String status);
    

    // Reset the status of failed contracts
    @Modifying
    @Query(value = """
        UPDATE dyn_disc_contract
        SET status = 'I'
        WHERE request_id = :requestId AND status IN ('I', 'F')
    """, nativeQuery = true)
    int resetFailedContracts(Integer requestId);
    

    // Retrieve contracts by request ID and status
    @Query(value = """
        SELECT *
        FROM dyn_disc_contract
        WHERE request_id = :requestId AND status = :status
    """, nativeQuery = true)
    List<DynDiscContract> getContractsByStatus(Integer requestId, String status);
    

    // Fetch all unprocessed contracts for a request ID
    @Query(value = """
        SELECT * 
        FROM dyn_disc_contract 
        WHERE request_id = :requestId AND status = 'I'
        ORDER BY co_id
    """, nativeQuery = true)
    List<DynDiscContract> fetchAllUnprocessedContracts(Integer requestId);
    

    // Reset failed and initial contracts
    @Modifying
    @Query(value = """
        UPDATE dyn_disc_contract
        SET status = 'I'
        WHERE request_id = :requestId AND status IN ('I', 'F')
    """, nativeQuery = true)
    int resetFailedAndInitialContracts(Integer requestId);

    // Assign new package IDs to contracts
    @Modifying
    @Query(value = """
        UPDATE dyn_disc_contract
        SET pack_id = CEIL(ROWNUM / :batchSize) + :nextPackIdStart - 1
        WHERE request_id = :requestId AND status = 'I'
    """, nativeQuery = true)
    int assignNewPackIds(Integer requestId, int batchSize, int nextPackIdStart);

    // Fetch contracts for a specific package
    @Query(value = """
        SELECT * 
        FROM dyn_disc_contract 
        WHERE pack_id = :packageId AND request_id = :requestId AND status = 'I'
    """, nativeQuery = true)
    List<DynDiscContract> fetchContractsForPackage(Integer packageId, Integer requestId);

    // Update contract status and remark
    @Transactional   
    @Modifying
    @Query(value = """
        UPDATE dyn_disc_contract
        SET status = :status, remark = :errMsg
        WHERE request_id = :requestId AND co_id = :coId
    """, nativeQuery = true)
    void updateContractStatusAndRemark(Integer requestId, Integer coId, String status, String errMsg);
    }