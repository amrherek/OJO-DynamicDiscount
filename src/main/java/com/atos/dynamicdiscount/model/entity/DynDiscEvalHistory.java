package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@Table(name = "DYN_DISC_EVAL_HISTORY")
@IdClass(DynDiscEvalHistoryId.class)
public class DynDiscEvalHistory implements Serializable {

	@Id
	@Column(name = "REQUEST_ID")
	private Integer requestId;

	@Id
	@Column(name = "ASSIGN_ID")
	private Long assignId;

	@Column(name = "CUSTOMER_ID")
	private Integer customerId;

	@Column(name = "CO_ID")
	private Integer coId;

	@Column(name = "BILL_PERIOD_END_DATE")
	private LocalDateTime billPeriodEndDate;

	@Column(name = "LBC_DATE")
	private LocalDateTime lbcDate;

	@Column(name = "PRGCODE")
	private String prgCode;

	@Column(name = "TMCODE")
	private Integer tmCode;

	@Column(name = "DISC_SNCODE")
	private Integer discSncode;

	@Column(name = "DISC_ID")
	private Integer discId;
	
	@Column(name = "OCC_SNCODE")
	private Integer occSncode;

	@Column(name = "OCC_GLCODE")
	private String occGlcode;
	
	@Column(name = "occ_remark")
	private String occRemark;

	@Column(name = "OFFER_SNCODE")
	private Integer offerSncode;

	@Column(name = "OFFER_PRICE")
	private Float offerPrice;

	@Column(name = "OFFER_VALID_FROM")
	private LocalDateTime offerValidFrom;

	@Column(name = "OFFER_STATUS")
	private String offerStatus;

	@Column(name = "ALO_SNCODE")
	private Integer aloSncode;

	@Column(name = "ALO_PRICE")
	private Float aloPrice;

	@Column(name = "ALO_VALID_FROM")
	private LocalDateTime aloValidFrom;

	@Column(name = "ALO_STATUS")
	private String aloStatus;

}
