package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscCustGrpExcl;
import com.atos.dynamicdiscount.model.entity.DynDiscCustGrpExclId;

@Repository
public interface DynDiscCustGrpExclRepository extends JpaRepository<DynDiscCustGrpExcl, DynDiscCustGrpExclId> {
    // Add custom query methods if needed
}
