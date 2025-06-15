package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscPackage;
import com.atos.dynamicdiscount.model.entity.DynDiscPackageId;

import jakarta.transaction.Transactional;

@Repository
public interface DynDiscPackageRepository extends JpaRepository<DynDiscPackage, DynDiscPackageId> {

	
	@Query(value = """
			SELECT COUNT(*)
			  FROM dyn_disc_package
			 WHERE request_id = :requestId
			   AND status     = :status
			""", nativeQuery = true)
	long countByReqIdAndStatus(@Param("requestId") Integer requestId, @Param("status") String status);

	
	
	@Modifying
	@Query(value = """
	    UPDATE dyn_disc_package 
	    SET STATUS = 'P' 
	    WHERE REQUEST_ID = :requestId 
	      AND STATUS <> 'P'
	    """, 
	    nativeQuery = true)
	int updatePackagesToP(@Param("requestId") Integer requestId);



	@Query(value = """
		    SELECT MAX(pack_id)
		    FROM dyn_disc_package
		    WHERE request_id = :requestId
		    """, nativeQuery = true)
		Integer findMaxPackIdByRequestId(@Param("requestId") Integer requestId);
	
	@Modifying
	@Query(nativeQuery = true, value = """
	    -- Insert Packages
	    INSERT INTO dyn_disc_package (pack_id, request_id, contract_count)
	    SELECT 
	      DISTINCT pack_id,
	      :requestId AS request_id,
	      COUNT(*) OVER (PARTITION BY pack_id) AS contract_count
	    FROM dyn_disc_contract
	    WHERE request_id = :requestId
	    ORDER BY pack_id
	    """)
	int insertPackages(@Param("requestId") Integer requestId);

	


	@Modifying
	@Query(value = """
	    INSERT INTO dyn_disc_package (pack_id, request_id, contract_count, status)
	    SELECT pack_id, :requestId, COUNT(*), 'I'
	    FROM dyn_disc_contract
	    WHERE request_id = :requestId
	      AND status = 'I'
	    GROUP BY pack_id
	    ORDER BY pack_id
	    """, nativeQuery = true)
	int insertNewPackagesForUpdatedContracts(@Param("requestId") Integer requestId);





	    @Query(value = """
	                   SELECT pack_id 
	                   FROM dyn_disc_package 
	                   WHERE status = :status 
	                   AND request_id = :requestId
	                   order by pack_id  
	                   """, nativeQuery = true)
	    List<Integer> fetchAvailablePackagesWithStatus(@Param("requestId") Integer requestId,@Param("status") String status);



	    
	    @Modifying
	    @Transactional
	    @Query(value = "UPDATE dyn_disc_package " +
	                   "SET status = :status, " +
	                   "    end_date = :endDate " +
	                   "WHERE pack_id = :packageId", 
	           nativeQuery = true)
	    void updatePackageStatusAndEndDate(
	            @Param("packageId") Integer packageId, 
	            @Param("status") String status, 
	            @Param("endDate") LocalDateTime endDate);

	    

	    
	    @Modifying
	    @Transactional
	    @Query(value = "UPDATE dyn_disc_package " +
	                   "SET status = :status, " +
	                   "    start_date = :startDate " +
	                   "WHERE pack_id = :packageId", 
	           nativeQuery = true)
	    void updatePackageStatusAndStartDate(
	            @Param("packageId") Integer packageId, 
	            @Param("status") String status, 
	            @Param("startDate") LocalDateTime startDate);


	




}
