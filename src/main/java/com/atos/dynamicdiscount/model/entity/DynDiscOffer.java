package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "DYN_DISC_OFFER")
@Data
@NoArgsConstructor
@ToString

public class DynDiscOffer {

	@Id
	@Column(name = "offer_id", nullable = false)
	private Integer offerId;

	@Column(name = "disc_id")
	private Integer discId;

	@Column(name = "tmcode")
	private Integer tmcode;

	@Column(name = "sncode")
	private Integer sncode;

	@Column(name = "offer_disc_amt")
	private Float offerDiscAmt;

	@Column(name = "alo_disc_amt")
	private Float aloDiscAmt;
	
	@Column(name = "elig_start_date")
	private LocalDateTime eligStartDate;
	
	@Column(name = "elig_end_date")
	private LocalDateTime eligEndDate;

	@Column(name = "free_month_ind")
	private Boolean freeMonthInd;

	@Column(name = "special_month_ind")
	private Boolean specialMonthInd;
}
