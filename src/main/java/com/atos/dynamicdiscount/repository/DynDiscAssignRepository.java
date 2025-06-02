package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.dto.DynDiscAssignDTO;
import com.atos.dynamicdiscount.model.entity.DynDiscAssign;

@Repository
public interface DynDiscAssignRepository extends JpaRepository<DynDiscAssign, Long> {

	@Query(value = "SELECT * FROM dyn_disc_assign WHERE assign_id = (SELECT MAX(assign_id) FROM dyn_disc_assign WHERE co_id = :coId AND disc_sncode = :discSncode)", nativeQuery = true)
	Optional<DynDiscAssign> findLatestAssign(@Param("coId") Integer coId, @Param("discSncode") Integer discSncode);
	
	
	

	
	
	@Query(nativeQuery = true,value = """
			WITH
			  /*--------------------------------------------
			   1) Fetch Valid Discount Assignments
			      - Only assignments for the given coIds
			      - Active at or before the target date
			      - Pull tmcode, lbc_date, prgcode from contract
			  --------------------------------------------*/
			  valid_assigns AS (
			    SELECT
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
			      dyc.tmcode
			    FROM dyn_disc_assign   d
			    JOIN dyn_disc_contract dyc
			      ON d.co_id = dyc.co_id
				  AND dyc.request_id=:requestId
				  AND dyc.status='I'
			    WHERE dyc.co_id      IN (:coIds)
				  AND d.assign_date  < :targetDate
				  AND (d.delete_date IS NULL OR d.delete_date >= :targetDate)
				  AND (d.expire_date IS NULL OR d.expire_date >= :targetDate)
				  AND (d.last_applied_date is NULL or d.last_applied_date < :targetDate)
			  ),
			
			  /*--------------------------------------------
			   2) Latest Offer History
			      - Join to valid dyn_disc_Assigns and pre_serv_Status_hist for MCD WAN offers
			      - Pick the most recent row per (assign_id, sncode)
			  --------------------------------------------*/
			  offer_history_ranked AS (
			    SELECT
			      va.*,
			      ph.sncode           AS offer_sncode,
			      ph.status           AS offer_status,
			      ph.valid_from_date  AS offer_valid_from_date,
			      ph.histno           AS offer_histno,
			      ROW_NUMBER() OVER (
			        PARTITION BY va.assign_id, ph.sncode
			        ORDER BY ph.valid_from_date DESC, ph.histno DESC
			      ) AS rn_offer_sn
			    FROM valid_assigns va
			    JOIN pr_serv_status_hist ph
			      ON ph.co_id           = va.co_id
				 AND ph.valid_from_date <= :targetDate
			    JOIN mcd_wan_offers m
			      ON m.tmcode = va.tmcode
			     AND m.sncode = ph.sncode
			  ),
			
			  offer_history_latest AS (
			    SELECT *
			    FROM offer_history_ranked
			    WHERE rn_offer_sn = 1
			  ),
			
			  /*--------------------------------------------
			   3) Consolidate to One Offer per Assignment
			      - Prefer status A/S over others
			      - Then most recent valid_from_date/histno
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
			   4) Identify ALO Dependent Services
			      - Map offer services to their dependent ALO services
			  --------------------------------------------*/
			  offer_alo_view AS (
			    SELECT
			      sc.sncode           AS offer_sncode,
			      sc.dependent_sncode AS alo_sncode
			    FROM srv_service_consistency sc
			    WHERE sc.dependency_type = 'N'
			      AND EXISTS (
			        SELECT 1
			        FROM mpusntab mp
			        WHERE mp.sncode     = sc.dependent_sncode
			          AND LOWER(mp.des) LIKE '%alo%'
			          AND mp.shdes      LIKE 'WA%'
			      )
			  ),
			
			  /*--------------------------------------------
			   5) Include Latest ALO Services
			      - Retrieve the most recent ALO service data
			  --------------------------------------------*/
			  alo_history_ranked AS (
			    SELECT
			      va.*,
			      ph.sncode           AS alo_sncode,
			      ph.status           AS alo_status,
			      ph.valid_from_date  AS alo_valid_from_date,
			      ph.histno           AS alo_histno,
			      ROW_NUMBER() OVER (
			        PARTITION BY va.assign_id, ph.sncode
			        ORDER BY ph.valid_from_date DESC, ph.histno DESC
			      ) AS rn_sn
			    FROM valid_assign_with_offer va
			    LEFT JOIN offer_alo_view ov
			      ON va.offer_sncode = ov.offer_sncode
			    LEFT JOIN pr_serv_status_hist ph
			      ON ph.co_id           = va.co_id
			     AND ph.sncode          = ov.alo_sncode
				 AND ph.valid_from_date <=:targetDate
			  ),
			
			  alo_history_latest AS (
			    SELECT *
			    FROM alo_history_ranked
			    WHERE rn_sn = 1
			  ),
			
			  /*--------------------------------------------
			   6) Combine Offer + ALO per Assignment
			  --------------------------------------------*/
			  alo_ranked AS (
			    SELECT
			      h.*,
			      ROW_NUMBER() OVER (
			        PARTITION BY h.assign_id
			        ORDER BY
			          CASE
			            WHEN h.alo_status IN ('A', 'S') THEN 1
			            WHEN h.alo_status = 'D'           THEN 2
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
			   7) Attach Offer Pricing
			      - Use override fee if in override period
			      - Otherwise use latest tm.accessfee as of targetDate
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
			        ps.ovw_acc_prd    AS offer_ovw_acc_prd,
			        ps.accessfee      AS offer_ps_accessfee,
			        tm.accessfee      AS offer_tm_accessfee,
			        ROW_NUMBER() OVER (
			          PARTITION BY va.assign_id,tm.tmcode, tm.spcode, tm.sncode
			          ORDER BY tm.vsdate DESC
			        ) AS rn
			      FROM va_with_offer_and_alo va
			      JOIN profile_service ps
			        ON ps.co_id   = va.co_id
			       AND ps.sncode = va.offer_sncode
			      JOIN pr_serv_spcode_hist psp
			        ON psp.co_id   = ps.co_id
			       AND psp.sncode  = ps.sncode
			       AND psp.histno = ps.spcode_histno
			      JOIN mpulktmb tm
			        ON tm.tmcode  = va.tmcode
			       AND tm.spcode  = psp.spcode
			       AND tm.sncode  = va.offer_sncode
				   AND tm.vsdate <= :targetDate
			    ) x
			    WHERE x.rn = 1
			  ),
			
			  /*--------------------------------------------
			   8) Attach ALO Pricing
			      - Same override logic for ALO services
			  --------------------------------------------*/
			  va_with_alo_fee AS (
			    SELECT
			      x.*,
			      CASE
			        WHEN ABS(x.alo_ovw_acc_prd) >= 1 THEN x.alo_ps_accessfee
			        ELSE x.alo_tm_accessfee
			      END AS alo_price
			    FROM (
			      SELECT
			        va.*,
			        ps.ovw_acc_prd    AS alo_ovw_acc_prd,
			        ps.accessfee      AS alo_ps_accessfee,
			        tm.accessfee      AS alo_tm_accessfee,
			        ROW_NUMBER() OVER (
			          PARTITION BY va.assign_id,tm.tmcode, tm.spcode, tm.sncode
			          ORDER BY tm.vsdate DESC
			        ) AS rn1
			      FROM va_with_offer_fee va
			      LEFT JOIN profile_service ps
			        ON ps.co_id   = va.co_id
			       AND ps.sncode = va.alo_sncode
			      LEFT JOIN pr_serv_spcode_hist psp
			        ON psp.co_id   = ps.co_id
			       AND psp.sncode  = ps.sncode
			       AND psp.histno = ps.spcode_histno
			      LEFT JOIN mpulktmb tm
			        ON tm.tmcode  = va.tmcode
			       AND tm.spcode  = psp.spcode
			       AND tm.sncode  = va.alo_sncode
				   AND tm.vsdate <= :targetDate
			    ) x
			    WHERE x.rn1 = 1
			  )
			
			SELECT
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
			  va.offer_sncode       AS offer_sncode,
			  va.offer_valid_from_date AS offervalidfromdate,
			  va.offer_status       AS offerstatus,
			  vof.offer_price       AS offerprice,
			  va.alo_sncode         AS alosncode,
			  va.alo_valid_from_date   AS alovalidfromdate,
			  va.alo_status         AS alostatus,
			  vaf.alo_price         AS aloprice
			FROM va_with_alo_fee vaf
			JOIN va_with_offer_fee vof
			  ON vof.assign_id = vaf.assign_id
			 AND vof.co_id     = vaf.co_id
			JOIN va_with_offer_and_alo va
			  ON va.assign_id = vaf.assign_id
			 AND va.co_id     = vaf.co_id
			ORDER BY va.assign_id
			""")
	List<DynDiscAssignDTO> fetchAssignedDiscounts(@Param("coIds") List<Integer> coIds, Integer requestId, @Param("targetDate") LocalDateTime targetDate);	
}