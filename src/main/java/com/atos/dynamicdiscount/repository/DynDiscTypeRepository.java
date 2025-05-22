package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscType;

@Repository
public interface DynDiscTypeRepository extends JpaRepository<DynDiscType, Integer> {
    // Add custom query methods if needed
}
