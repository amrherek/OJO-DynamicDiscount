package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscRequest;

@Repository
public interface DynDiscRequestRepository extends JpaRepository<DynDiscRequest, Integer> {

	@Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END " + "FROM DYN_DISC_REQUEST "
			+ "WHERE status = :status", nativeQuery = true)
	int countRequestsByStatus(@Param("status") String status);

	// Method to get the next available request_id
	@Query(value = "SELECT REQUEST_ID_SEQUENCE.NEXTVAL FROM DUAL", nativeQuery = true)
	Integer getNextAvailableRequestId();

	@Modifying
	@Query(value = """
			UPDATE dyn_disc_request
			   SET status   = :newStatus,
			       status_date = :statusDate
			 WHERE request_id = :requestId
			""", nativeQuery = true)
	int updateStatusAndEndDate(@Param("requestId") Integer requestId, @Param("newStatus") String newStatus,
			@Param("statusDate") LocalDateTime statusDate);

	List<DynDiscRequest> findByStatus(String string);
}
