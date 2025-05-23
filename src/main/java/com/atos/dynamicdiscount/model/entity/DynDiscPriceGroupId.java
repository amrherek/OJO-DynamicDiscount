package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Composite primary key for the DynDiscCustGrpExcl entity.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DynDiscPriceGroupId implements Serializable {

    @Column(name = "disc_id", nullable = false)
    private Integer discId;

    @Column(name = "prgcode", nullable = false)
    private String prgcode;

    // Add constructors if needed
}
