package com.atos.dynamicdiscount.model.entity;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynDiscGrantHistoryId implements Serializable {
    private Integer requestId;
    private Long assignId;
}
