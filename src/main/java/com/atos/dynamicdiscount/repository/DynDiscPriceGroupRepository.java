package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroup;
import com.atos.dynamicdiscount.model.entity.DynDiscPriceGroupId;

@Repository
public interface DynDiscPriceGroupRepository extends JpaRepository<DynDiscPriceGroup, DynDiscPriceGroupId> {
}
