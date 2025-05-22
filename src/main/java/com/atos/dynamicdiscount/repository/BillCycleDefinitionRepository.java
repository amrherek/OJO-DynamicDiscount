package com.atos.dynamicdiscount.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.atos.dynamicdiscount.model.entity.BillCycleDefinition;

public interface BillCycleDefinitionRepository extends JpaRepository<BillCycleDefinition, String> {

	// Query to fetch the target run date by billCycle
	@Query(value = "SELECT BCH_RUN_DATE FROM BILLCYCLE_DEFINITION WHERE billcycle = :billCycle", nativeQuery = true)
	LocalDateTime findTargetRunDateByBillCycle(String billCycle);
}
