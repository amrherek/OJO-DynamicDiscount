package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscOffer;

@Repository
public interface DynDiscOfferRepository extends JpaRepository<DynDiscOffer, Integer> {
    // Add custom query methods if needed
}
