package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscAssign;

@Repository
public interface DynDiscAssignRepository extends JpaRepository<DynDiscAssign, Long> {

	@Query(value = "SELECT * FROM dyn_disc_assign WHERE assign_id = (SELECT MAX(assign_id) FROM dyn_disc_assign WHERE DELETE_DATE is NULL and co_id = :coId AND disc_sncode = :discSncode)", nativeQuery = true)
	Optional<DynDiscAssign> findLatestAssign(@Param("coId") Integer coId, @Param("discSncode") Integer discSncode);
	


	

	
	@Query(nativeQuery = true,value = """
			WITH
			  /*--------------------------------------------
			   1) Fetch Valid Discount Assignments
			      - Only assignments for the given coIds
			      - Active at or before the target date
			      - Include tmcode, lbc_date, prgcode from contract
			  --------------------------------------------*/
			  valid_assigns AS (
			    SELECT
				 /*+ parallel(d, 4) */
			      d.assign_id,
			      d.assign_date,
			      d.disc_sncode,
			      d.disc_id,
			      NVL(d.apply_count, 0) AS apply_count,
			      d.ovw_apply_count,
			      d.customer_id,
			      d.co_id,
			      dyc.lbc_date,
			      dyc.prgcode,
			      dyc.tmcode,
			      dyc.request_id
			    FROM dyn_disc_assign d
			    JOIN dyn_disc_contract dyc
			      ON d.co_id = dyc.co_id
			     AND dyc.request_id = :requestId
			     AND dyc.pack_id=:packId
			     AND dyc.status = 'I'
			    WHERE d.assign_date < :targetDate
			      AND (d.delete_date IS NULL OR d.delete_date >= :targetDate)
			      AND (d.expire_date IS NULL OR d.expire_date >= :targetDate)
			      AND (d.last_applied_date IS NULL OR d.last_applied_date < :targetDate)
			  ),
			
			  /*--------------------------------------------
			   2) Ranked Offer History
			      - Filter `pr_serv_status_hist` for relevant records
			      - Join with valid assignments
			      - Join with `mcd_wan_offers` for offer details
			      - Assign row numbers for ranking by date and histno
			  --------------------------------------------*/
			  filtered_ph AS (
			    SELECT 
			      ph.co_id, ph.sncode, ph.status, ph.valid_from_date, ph.histno
			    FROM pr_serv_status_hist ph
			    WHERE ph.valid_from_date <= :targetDate
			      AND EXISTS (
			        SELECT 1 
			        FROM valid_assigns va 
			        WHERE va.co_id = ph.co_id
			      )
			  ),
			
			  offer_history_ranked AS (
			    SELECT 
			      va.*, 
			      ph.sncode AS offer_sncode, 
			      ph.status AS offer_status,
			      ph.valid_from_date AS offer_valid_from_date,
			      ph.histno AS offer_histno,
			      ROW_NUMBER() OVER (
			        PARTITION BY va.assign_id, ph.sncode 
			        ORDER BY ph.valid_from_date DESC, ph.histno DESC
			      ) AS rn_offer_sn
			    FROM valid_assigns va 
			    JOIN filtered_ph ph
			      ON ph.co_id = va.co_id
			    JOIN mcd_wan_offers m
			      ON m.tmcode = va.tmcode AND m.sncode = ph.sncode
			  ),
			
			  offer_history_latest AS (
			    SELECT *
			    FROM offer_history_ranked
			    WHERE rn_offer_sn = 1
			  ),
			
			  /*--------------------------------------------
			   3) Consolidate Offers
			      - Prefer status A/S over others
			      - Use the most recent valid_from_date and histno
			  --------------------------------------------*/
			  offer_ranked AS (
			    SELECT
			      h.*,
			      ROW_NUMBER() OVER (
			        PARTITION BY h.assign_id
			        ORDER BY
			          CASE WHEN h.offer_status IN ('A', 'S') THEN 1 ELSE 2 END,
			          h.offer_valid_from_date DESC,
			          h.offer_histno DESC
			      ) AS rn_offer
			    FROM offer_history_latest h
			  ),
			
			  valid_assign_with_offer AS (
			    SELECT *
			    FROM offer_ranked
			    WHERE rn_offer = 1
			  ),
			
			  /*--------------------------------------------
			   4) Map Offers to ALO Dependent Services
			  --------------------------------------------*/
			  offer_alo_view AS (
			    SELECT
				 /*+ parallel(sc, 4) */
			      sc.sncode AS offer_sncode,
			      sc.dependent_sncode AS alo_sncode
			    FROM srv_service_consistency sc
			    WHERE sc.dependency_type = 'N'
			      AND EXISTS (
			        SELECT 1
			        FROM mpusntab mp
			        WHERE mp.sncode = sc.dependent_sncode
			          AND LOWER(mp.des) LIKE '%alo%'
			          AND mp.shdes LIKE 'WA%'
			      )
			  ),
			
			  /*--------------------------------------------
			   5) Include Latest ALO Service Details
			  --------------------------------------------*/
			  alo_history_ranked AS (
			    SELECT
			      va.*,
			      ph.sncode AS alo_sncode,
			      ph.status AS alo_status,
			      ph.valid_from_date AS alo_valid_from_date,
			      ph.histno AS alo_histno,
			      ROW_NUMBER() OVER (
			        PARTITION BY va.assign_id, ph.sncode
			        ORDER BY ph.valid_from_date DESC, ph.histno DESC
			      ) AS rn_sn
			    FROM valid_assign_with_offer va
			    LEFT JOIN offer_alo_view ov
			      ON va.offer_sncode = ov.offer_sncode
			    LEFT JOIN pr_serv_status_hist ph
			      ON ph.co_id = va.co_id
			     AND ph.sncode = ov.alo_sncode
			     AND ph.valid_from_date <= :targetDate
			  ),
			
			  alo_history_latest AS (
			    SELECT *
			    FROM alo_history_ranked
			    WHERE rn_sn = 1
			  ),
			
			  /*--------------------------------------------
			   6) Combine Offers and ALO Services
			  --------------------------------------------*/
			  alo_ranked AS (
			    SELECT
			      h.*,
			      ROW_NUMBER() OVER (
			        PARTITION BY h.assign_id
			        ORDER BY
			          CASE
			            WHEN h.alo_status IN ('A', 'S') THEN 1
			            WHEN h.alo_status = 'D' THEN 2
			            ELSE 3
			          END,
			          h.alo_valid_from_date DESC,
			          h.alo_histno DESC
			      ) AS rn_alo
			    FROM alo_history_latest h
			  ),
			
			  va_with_offer_and_alo AS (
			    SELECT *
			    FROM alo_ranked
			    WHERE rn_alo = 1
			  ),
			
			  /*--------------------------------------------
			   7) Enrich with Offer Price Details
			      - Use standard or overridden prices
			  --------------------------------------------*/
			  va_with_offer_fee AS (
			    SELECT
			      x.*,
			      CASE
			        WHEN ABS(x.offer_ovw_acc_prd) >= 1 THEN x.offer_ps_accessfee
			        ELSE x.offer_tm_accessfee
			      END AS offer_price
			    FROM (
			      SELECT
			        va.*,
			        ps.ovw_acc_prd AS offer_ovw_acc_prd,
			        ps.accessfee AS offer_ps_accessfee,
			        tm.accessfee AS offer_tm_accessfee,
			        ROW_NUMBER() OVER (
			          PARTITION BY va.assign_id, tm.tmcode, tm.spcode, tm.sncode
			          ORDER BY tm.vsdate DESC
			        ) AS rn
			      FROM va_with_offer_and_alo va
			      JOIN profile_service ps
			        ON ps.co_id = va.co_id
			       AND ps.sncode = va.offer_sncode
			      JOIN pr_serv_spcode_hist psp
			        ON psp.co_id = ps.co_id
			       AND psp.sncode = ps.sncode
			       AND psp.histno = ps.spcode_histno
			      JOIN mpulktmb tm
			        ON tm.tmcode = va.tmcode
			       AND tm.spcode = psp.spcode
			       AND tm.sncode = va.offer_sncode
			       AND tm.vsdate <= :targetDate
			    ) x
			    WHERE x.rn = 1
			  ),
			
			  /*--------------------------------------------
			   8) Enrich with ALO Price Details
			      - Use standard or overridden prices
			  --------------------------------------------*/
			  va_offer_alo_full_view AS (
			    SELECT
			      x.*,
			      CASE
			        WHEN ABS(x.alo_ovw_acc_prd) >= 1 THEN x.alo_ps_accessfee
			        ELSE x.alo_tm_accessfee
			      END AS alo_price
			    FROM (
			      SELECT
			        va.*,
			        ps.ovw_acc_prd AS alo_ovw_acc_prd,
			        ps.accessfee AS alo_ps_accessfee,
			        tm.accessfee AS alo_tm_accessfee,
			        ROW_NUMBER() OVER (
			          PARTITION BY va.assign_id, tm.tmcode, tm.spcode, tm.sncode
			          ORDER BY tm.vsdate DESC
			        ) AS rn1
			      FROM va_with_offer_fee va
			      LEFT JOIN profile_service ps
			        ON ps.co_id = va.co_id
			       AND ps.sncode = va.alo_sncode
			      LEFT JOIN pr_serv_spcode_hist psp
			        ON psp.co_id = ps.co_id
			       AND psp.sncode = ps.sncode
			       AND psp.histno = ps.spcode_histno
			      LEFT JOIN mpulktmb tm
			        ON tm.tmcode = va.tmcode
			       AND tm.spcode = psp.spcode
			       AND tm.sncode = va.alo_sncode
			       AND tm.vsdate <= :targetDate
			    ) x
			    WHERE x.rn1 = 1
			  )
			
			SELECT
			  va.request_id,
			  va.assign_id,
			  va.assign_date,
			  va.disc_sncode,
			  va.disc_id,
			  va.apply_count,
			  va.ovw_apply_count,
			  va.customer_id,
			  va.co_id,
			  va.lbc_date,
			  va.prgcode,
			  va.tmcode,
			  va.offer_sncode AS offer_sncode,
			  va.offer_valid_from_date AS offervalidfromdate,
			  va.offer_status AS offerstatus,
			  va.offer_price AS offerprice,
			  va.alo_sncode AS alosncode,
			  va.alo_valid_from_date AS alovalidfromdate,
			  va.alo_status AS alostatus,
			  va.alo_price AS aloprice
			FROM va_offer_alo_full_view va
						""")
			List<DynDiscAssignDTO> fetchDiscountsByPackage (@Param("packId") Integer packId,@Param("requestId")Integer requestId, @Param("targetDate") LocalDateTime targetDate);

	    
	    // Fetch contracts by request ID using native SQL
	    @Query(value = "SELECT * FROM dyn_disc_contract WHERE request_id = :requestId", nativeQuery = true)
	    List<DynDiscAssignDTO> findByRequestId(@Param("requestId") Integer requestId);








	
}