package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_CONF")
@Data
@NoArgsConstructor

public class DynDiscConf {

	@Id
	@Column(name = "disc_id")
	private Integer discId;

	@Column(name = "name")
	private String name;

	@Column(name = "description", length = 100)
	private String description;

	@Column(name = "disc_sncode")
	private Integer discSncode;

	@Column(name = "duration")
	private Integer duration;

	@Column(name = "offer_disc_amt")
	private float offerDiscAmt;

	@Column(name = "alo_disc_amt")
	private float aloDiscAmt;

	@Column(name = "occ_sncode")
	private Integer occSncode;

	@Column(name = "occ_glcode")
	private String occGlcode;

	@Column(name = "occ_remark")
	private String occRemark;

	@Column(name = "valid_from")
	private LocalDateTime validFrom;

	@Column(name = "valid_to")
	private LocalDateTime validTo;

	@Column(name = "alo_disc_ind")
	private Boolean aloDiscInd;

	@Column(name = "susp_ind")
	private Boolean suspInd;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "username")
	private String username;

}
