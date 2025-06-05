package com.atos.dynamicdiscount.model.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GmdRequestHistoryDTO {
	@Id
	private BigDecimal request;
	private BigDecimal customerId;
	private BigDecimal coId;
	private BigDecimal actionId;
	private Integer discId;
	private Integer discSncode;
	private Timestamp validFromDate;
}
