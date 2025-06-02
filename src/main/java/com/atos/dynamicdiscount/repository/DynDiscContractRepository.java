package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.DynDiscContractDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscContractId;

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
			      AND ca.co_id in (20549596,31173998,34225032)
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
			  - Retrieve required fields with a fixed status and error message
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
	List<DynDiscContractDTO> fetchEligibleContracts(@Param("targetDate") LocalDateTime cutoffDate,
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
			     AND status     = 'F'
			""", nativeQuery = true)
	int resetFailedContracts(@Param("requestId") Integer requestId);

	@Query(value = """
			  SELECT *
			    FROM dyn_disc_contract
			   WHERE request_id = :requestId
			     AND status = :status
			""", nativeQuery = true)
	List<DynDiscContract> getContractsByStatus(@Param("requestId") Integer requestId, @Param("status") String status);

}
