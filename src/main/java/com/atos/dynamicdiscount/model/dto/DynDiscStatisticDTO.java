package com.atos.dynamicdiscount.model.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DynDiscStatisticDTO {
    private BigDecimal requestId;
    private Date startDate;
    private Date endDate;
    private BigDecimal custCnt;
    private BigDecimal contrCnt;
    private BigDecimal contrGrantedCnt;
    private BigDecimal contrSkippedCnt;
    private BigDecimal offerOccCnt;
    private BigDecimal aloOccCnt;
    private BigDecimal offerOccAmt;
    private BigDecimal aloOccAmt;
}
