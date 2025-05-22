package com.atos.dynamicdiscount.model.entity;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynDiscEvalHistoryId implements Serializable {
    private Integer requestId;
    private Long assignId;
}
