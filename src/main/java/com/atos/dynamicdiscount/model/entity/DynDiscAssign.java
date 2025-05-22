package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_ASSIGN")
@Data
@NoArgsConstructor
public class DynDiscAssign {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assign_id_seq")
	@SequenceGenerator(name = "assign_id_seq", sequenceName = "assign_id_sequence", allocationSize = 1)
	@Column(name = "assign_id", nullable = false)
	private Long assignId;

	@Column(name = "customer_id", nullable = false)
	private Integer customerId;

	@Column(name = "co_id", nullable = false)
	private Integer coId;

	@Column(name = "disc_sncode", nullable = false)
	private Integer discSncode;

	@Column(name = "disc_id", nullable = false)
	private Integer discId;

	
	@Column(name = "entry_date")
	private LocalDateTime entryDate;
	
	@Column(name = "assign_date")
	private LocalDateTime assignDate;

	@Column(name = "delete_date")
	private LocalDateTime deleteDate;

	@Column(name = "expire_date")
	private LocalDateTime expireDate;

	@Column(name = "last_applied_date")
	private LocalDateTime lastAppliedDate;
	
	@Column(name = "apply_count")
	private Integer applyCount;
	
	@Column(name = "gmd_request")
	private Integer gmdRequest;

}
