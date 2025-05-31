package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "DYN_DISC_GRANT_HISTORY")
@IdClass(DynDiscGrantHistoryId.class)
public class DynDiscGrantHistory implements Serializable {

	@Id
	@Column(name = "REQUEST_ID")
	private Integer requestId;

	@Id
	@Column(name = "ASSIGN_ID")
	private Long assignId;

	@Column(name = "OFFER_DISC_AMOUNT")
	private Float offerDiscAmount;

	@Column(name = "FREE_MONTH")
	private Boolean freeMonth;

	@Column(name = "SPECIAL_MONTH")
	private Boolean specialMonth;

	@Column(name = "OFFER_CAPPED")
	private Boolean offerCapped;

	@Column(name = "CURRENT_APPLY_COUNT")
	private Integer currentApplyCount;

	@Column(name = "LAST_APPLY")
	private Boolean lastApply;

	@Column(name = "ALO_DISC_AMOUNT")
	private Float aloDiscAmount;

	@Column(name = "ALO_DISC_IND")
	private Boolean aloDiscInd;

	@Column(name = "ALO_CAPPED")
	private Boolean aloCapped;

	@Column(name = "NOTE")
	private String note;

	@Column(name = "OFFER_OCC_CREATED")
	private Boolean offerOccCreated;
	
	@Column(name = "ALO_OCC_CREATED")
	private Boolean aloOccCreated;
	
	@Column(name = "username")
	private String username;
}
