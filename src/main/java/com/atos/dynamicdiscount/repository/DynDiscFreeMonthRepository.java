package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscFreeMonthId;

@Repository
public interface DynDiscFreeMonthRepository extends JpaRepository<DynDiscFreeMonth, DynDiscFreeMonthId> {
    // Add custom query methods if needed
}
