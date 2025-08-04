package com.atos.dynamicdiscount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atos.dynamicdiscount.model.entity.DynDiscGmdQueueHist;

/**
 * Repository for DYN_DISC_GMD_QUEUE_HIST table.
 */
@Repository
public interface DynDiscGmdQueueHistRepository extends JpaRepository<DynDiscGmdQueueHist, Long> {
}
