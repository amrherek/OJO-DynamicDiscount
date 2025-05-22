package com.atos.dynamicdiscount.model.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing free month discounts.
 */
@Entity
@Table(name = "DYN_DISC_FREE_MONTH")
@Data
@NoArgsConstructor
public class DynDiscFreeMonth {

    @EmbeddedId
    private DynDiscFreeMonthId id;
}
