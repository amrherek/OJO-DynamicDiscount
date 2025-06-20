package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_REQUEST")
@Data
@NoArgsConstructor
public class DynDiscRequest {

	@Id
	@Column(name = "request_id")
	private Integer requestId;

	@Column(name = "status")
	private String status;

	@Column(name = "start_date", insertable = false, updatable = false)
	private LocalDateTime startDate;

	@Column(name = "status_date")
	private LocalDateTime statusDate;

	@Column(name = "billcycle", length = 2)
	private String billcycle;

	@Column(name = "bill_period_end_date")
	private LocalDateTime billPeriodEndDate;

	@Column(name = "stop_process_flag", insertable = false)
	private String stopProcessFlag;

}
