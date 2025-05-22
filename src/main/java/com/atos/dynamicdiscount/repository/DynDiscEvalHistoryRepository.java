package com.atos.dynamicdiscount.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistoryId;

@Repository
public interface DynDiscEvalHistoryRepository extends JpaRepository<DynDiscEvalHistory, DynDiscEvalHistoryId> {
    // Custom queries for DynDiscEvalHistory can be added here, if necessary
}
