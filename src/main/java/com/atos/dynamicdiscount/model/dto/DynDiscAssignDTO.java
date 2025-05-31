package com.atos.dynamicdiscount.model.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DynDiscAssignDTO {
	private BigDecimal assignId;
	private Date assignDate;
	private BigDecimal discSncode;
	private BigDecimal discId;
	private BigDecimal applyCount;
	private BigDecimal ovwApplyCount;
	private BigDecimal customerId;
	private BigDecimal coId;
	private Date lbcDate;
	private String prgcode;
	private Integer tmCode;
	private BigDecimal offerSnCode;
	private Date offerValidFromDate;
	private char offerStatus;
	private BigDecimal offerPrice;
	private BigDecimal aloSnCode;
	private Date aloValidFromDate;
	private char aloStatus;
	private BigDecimal aloPrice;

}