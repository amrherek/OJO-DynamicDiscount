package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscConf;

@Repository
public interface DynDiscConfRepository extends JpaRepository<DynDiscConf, Integer> {
    // Add custom query methods if needed
}
