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

	/*
	 * For Testing All actions please select all the requests for the following
	 * contracts in the filtered_requests view
	 * 34521501,28452930,21490603,32377476,34481248,28414961
	 */

	@Query(value = """
			    WITH filtered_requests AS (
			        SELECT
			            request,
			            customer_id,
			            co_id,
			            action_id
			        FROM
			            gmd_request_history
			        WHERE
			           request > :start
			           AND request <= :end
			           AND  action_id IN (1, 2, 3, 4, 5, 8, 9)
			          --For Testing -- AND co_id in (34521501,28452930,21490603,32377476,34481248,28414961)
			    )
			    SELECT
			        grh.request AS request,
			        grh.customer_id,
			        grh.co_id,
			        grh.action_id,
			        ddc.disc_id,
			        ddc.disc_sncode,
			        psh.valid_from_Date
			    FROM
			        filtered_requests grh
			    JOIN
			        pr_serv_status_hist psh ON grh.co_id = psh.co_id
			        AND grh.request=psh.request_id
			    JOIN
			        dyn_disc_conf ddc ON psh.sncode = ddc.disc_sncode
			    ORDER BY
			        grh.request ASC
			""", nativeQuery = true)
	List<GmdRequestHistoryDTO> findRequestsInRange(@Param("start") Long start, @Param("end") Long end);

	@Query(value = "SELECT MAX(REQUEST) FROM GMD_REQUEST_HISTORY", nativeQuery = true)
	Long findMaxRequest();
}
