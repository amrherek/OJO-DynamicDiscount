package com.atos.dynamicdiscount.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.atos.dynamicdiscount.model.entity.DynDiscFirstLoginTrack;

@Repository
public interface DynDiscFirstLoginTrackRepository extends JpaRepository<DynDiscFirstLoginTrack, Integer> {

    /**
     * 1️⃣ Insert delta from source table into tracking table
     */
	@Modifying
	@Transactional
	@Query(value = """
			MERGE INTO DYN_DISC_FIRST_LOGIN_TRACK target
			USING (
			    SELECT ic.CO_ID,
			           ic.FIRST_LOGIN,
			           ca.CUSTOMER_ID,
			           ca.TMCODE
			    FROM bscs_wd.internet_co_first_login ic
			    JOIN contract_all ca
			      ON ic.CO_ID = ca.CO_ID
			    WHERE TMCODE=618
			) src
			ON (target.CO_ID = src.CO_ID)
			WHEN NOT MATCHED THEN
			  INSERT (CO_ID, FIRST_LOGIN, CUSTOMER_ID, TMCODE, PROCESSED_FLG)
			  VALUES (src.CO_ID, src.FIRST_LOGIN, src.CUSTOMER_ID, src.TMCODE, 'N')
	""", nativeQuery = true)
	int insertDeltaFromSource();


    /**
     * 2️⃣ Retrieve all unprocessed requests (PROCESSED_FLG = 'N')
     */
    List<DynDiscFirstLoginTrack> findByProcessedFlg(String processedFlg);

    /**
     * 3️⃣ Update processing status, processing date, and comments for a specific CO_ID using native query
     */
    @Modifying
    @Query(value = """
        UPDATE DYN_DISC_FIRST_LOGIN_TRACK
        SET PROCESSED_FLG = :processedFlg,
            PROCESSED_DATE = :processedDate,
            COMMENTS = :comments
        WHERE CO_ID = :coId
    """, nativeQuery = true)
    int updateProcessingStatus(Integer coId, String processedFlg, 
                               java.time.LocalDateTime processedDate, String comments);
}
