package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscAssignState;
import com.atos.dynamicdiscount.model.entity.DynDiscAssignStateId;

@Repository
public interface DynDiscAssignStateRepository extends JpaRepository<DynDiscAssignState, DynDiscAssignStateId> {
    @Query("SELECT COALESCE(MAX(d.seqno), 0) FROM DynDiscAssignState d WHERE d.assignId = :assignId")
    Integer findMaxSeqnoByAssignId(@Param("assignId") Long assignId);
}
