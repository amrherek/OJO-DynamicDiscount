package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonth;
import com.atos.dynamicdiscount.model.entity.DynDiscSpecialMonthId;

@Repository
public interface DynDiscSpecialMonthRepository extends JpaRepository<DynDiscSpecialMonth, DynDiscSpecialMonthId> {
    // Add custom query methods if needed
}
