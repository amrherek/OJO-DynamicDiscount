package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Composite primary key for the DynDiscFreeMonth entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DynDiscFreeMonthId implements Serializable {

    @Column(name = "offer_id", nullable = false)
    private Integer offerId;

    @Column(name = "month_no", nullable = false)
    private Integer monthNo;

    // Add constructors if required
}
