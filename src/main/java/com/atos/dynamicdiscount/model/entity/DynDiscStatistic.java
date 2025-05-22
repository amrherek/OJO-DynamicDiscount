package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "dyn_disc_statistic")
public class DynDiscStatistic {
	@Id
	@Column(name = "REQUEST_ID", nullable = false)
	private Integer requestId;

	@Column(name = "START_DATE", nullable = false)
	private LocalDateTime startDate;

	@Column(name = "END_DATE")
	private LocalDateTime endDate;

	@Column(name = "CUST_CNT")
	private Integer custCnt;

	@Column(name = "CONTR_CNT")
	private Integer contrCnt;

	@Column(name = "CONTR_GRANTED_CNT")
	private Integer contrGrantedCnt;

	@Column(name = "CONTR_SKIPPED_CNT")
	private Integer contrSkippedCnt;

	@Column(name = "OFFER_OCC_CNT")
	private Integer offerOccCnt;

	@Column(name = "ALO_OCC_CNT")
	private Integer aloOccCnt;

	@Column(name = "OFFER_OCC_AMT")
	private Float offerOccAmt;

	@Column(name = "ALO_OCC_AMT")
	private Float aloOccAmt;
}