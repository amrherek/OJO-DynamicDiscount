package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Composite primary key for the DynDiscSpecialMonth entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynDiscSpecialMonthId implements Serializable {

    @Column(name = "offer_id", nullable = false)
    private Integer offerId;

    @Column(name = "month_no", nullable = false)
    private Integer monthNo;

}
