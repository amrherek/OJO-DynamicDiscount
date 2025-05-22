package com.atos.dynamicdiscount.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistoryId;

@Repository
public interface DynDiscGrantHistoryRepository extends JpaRepository<DynDiscGrantHistory, DynDiscGrantHistoryId> {
    // Custom queries for DynDiscGrantHistory can be added here, if necessary
}
