package com.atos.dynamicdiscount.model.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynDiscContractDTO {
    private BigDecimal customerId;
    private BigDecimal coId;
    private Date lbcDate;
    private String prgcode;
    private BigDecimal tmcode;
}