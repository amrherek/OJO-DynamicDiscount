package com.atos.dynamicdiscount.model.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing customer group exclusions for dynamic discounts.
 */
@Entity
@Table(name = "DYN_DISC_CUST_GRP_EXCL")
@Data
@NoArgsConstructor

public class DynDiscCustGrpExcl {

	@EmbeddedId
	private DynDiscCustGrpExclId id;

}
