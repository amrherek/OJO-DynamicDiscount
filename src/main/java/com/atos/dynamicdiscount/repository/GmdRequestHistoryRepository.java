package com.atos.dynamicdiscount.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.GmdRequestHistoryDTO;
import com.atos.dynamicdiscount.model.entity.GmdRequestHistory;

@Repository
public interface GmdRequestHistoryRepository extends JpaRepository<GmdRequestHistory, Long> {



	@Query(value = """		
				WITH filtered_requests AS (
				    SELECT
				        request,
				        customer_id,
				        co_id,
				        action_id
				    FROM
				        gmd_request_history gmd
				    WHERE
				        request > :start
				        AND request <= :end
				        AND action_id IN (1, 2, 3, 4, 5, 8, 9)
				        AND NOT EXISTS (
				            SELECT 1 
				            FROM DYN_DISC_ASSIGN DA 
				            WHERE DA.gmd_request = gmd.request
				        )
				        AND NOT EXISTS (
				            SELECT 1 
				            FROM DYN_DISC_ASSIGN_STATE DAS 
				            WHERE DAS.gmd_request = gmd.request
				        )
				        -- For Testing
				        -- AND co_id IN (34521501, 28452930, 21490603, 32377476, 34481248, 28414961)
				)
				SELECT
				    grh.request AS request,
				    grh.customer_id AS customerId,
				    grh.co_id AS coId,
				    grh.action_id AS actionId,
				    ddc.disc_id AS discId,
				    ddc.disc_sncode AS discSncode,
				    psh.valid_from_Date AS validFromDate
				FROM
				    filtered_requests grh
				JOIN
				    pr_serv_status_hist psh 
				    ON grh.co_id = psh.co_id
				    AND grh.request = psh.request_id
				JOIN
				    dyn_disc_conf ddc 
				    ON psh.sncode = ddc.disc_sncode
				ORDER BY
				    grh.request ASC
			""", nativeQuery = true)
	List<GmdRequestHistoryDTO> findRequestsInRange(@Param("start") Long start, @Param("end") Long end);
	
	// Very important for tracking and fixing mismatch types
	//List<Object[]>  findRequestsInRange(@Param("start") Long start, @Param("end") Long end);

	@Query(value = "SELECT MAX(REQUEST) FROM GMD_REQUEST_HISTORY", nativeQuery = true)
	Long findMaxRequest();
}
