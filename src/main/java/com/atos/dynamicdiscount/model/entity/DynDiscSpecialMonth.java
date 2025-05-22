package com.atos.dynamicdiscount.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a special month for discounts.
 */
@Entity
@Table(name = "DYN_DISC_SPECIAL_MONTH")
@Data
@NoArgsConstructor

public class DynDiscSpecialMonth {

    @EmbeddedId
    private DynDiscSpecialMonthId id;

    @Column(name = "offer_disc_amt")
    private float offerDiscAmt;
    
    @Column(name = "aloDiscAmt")
    private float aloDiscAmt;
}
