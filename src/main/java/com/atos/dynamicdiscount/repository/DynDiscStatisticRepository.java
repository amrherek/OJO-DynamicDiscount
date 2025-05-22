package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscStatistic;

@Repository
public interface DynDiscStatisticRepository extends JpaRepository<DynDiscStatistic, Integer> {

	@Query(nativeQuery = true, value = """
		    SELECT
		        :requestId AS REQUEST_ID,
		        re.START_DATE AS START_DATE,
		        NULL AS END_DATE,
		        COUNT(DISTINCT co.customer_id) AS CUST_CNT,
		        COUNT(DISTINCT co.co_id) AS CONTR_CNT,
		        COUNT(DISTINCT CASE WHEN co.status = 'P' THEN co.co_id END) AS CONTR_GRANTED_CNT,
		        COUNT(DISTINCT CASE WHEN co.status = 'S' THEN co.co_id END) AS CONTR_SKIPPED_CNT,
		        SUM(CASE WHEN gr.offer_occ_created = 1 THEN 1 ELSE 0 END) AS OFFER_OCC_CNT,
		        SUM(CASE WHEN gr.alo_occ_created = 1 THEN 1 ELSE 0 END) AS ALO_OCC_CNT,
		        SUM(CASE WHEN gr.offer_occ_created = 1 THEN gr.offer_disc_amount ELSE 0 END) AS OFFER_OCC_AMT,
		        SUM(CASE WHEN gr.alo_occ_created = 1 THEN gr.alo_disc_amount ELSE 0 END) AS ALO_OCC_AMT
		    FROM
		        dyn_disc_request re
		    INNER JOIN
		        dyn_disc_contract co
		        ON re.request_id = co.request_id
		    LEFT JOIN
		        dyn_disc_eval_history ev
		        ON co.request_id = ev.request_id
		        AND co.co_id = ev.co_id
		    LEFT JOIN
		        dyn_disc_grant_history gr
		        ON co.request_id = gr.request_id
		        AND ev.assign_id = gr.assign_id
		    WHERE
		        co.request_id = :requestId
		    GROUP BY
		        re.request_id, re.START_DATE
		""")
		DynDiscStatistic getStatsByRequestId(@Param("requestId") Integer requestId);
	
}
