package com.atos.dynamicdiscount.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing customer group exclusions for dynamic discounts.
 */
@Entity
@Table(name = "DYN_DISC_PRICE_GROUP")
@Data
@NoArgsConstructor

public class DynDiscPriceGroup {

	@EmbeddedId
	private DynDiscPriceGroupId id;

	@Column(name = "RESTRICT_IND", nullable = false)
	private boolean restrictInd;

	@Column(name = "PROHIBIT_IND", nullable = false)
	private boolean prohibitInd;

}
