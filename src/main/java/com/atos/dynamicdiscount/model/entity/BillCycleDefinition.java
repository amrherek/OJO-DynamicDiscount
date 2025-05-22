package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "BILLCYCLE_DEFINITION")
@Data
public class BillCycleDefinition {

	@Id
	@Column(name = "billcycle")
	private String billCycle;

	@Column(name = "BCH_RUN_DATE")
	private LocalDate targetRunDate;

}
