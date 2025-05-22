package com.atos.dynamicdiscount.model.entity;
import lombok.Data;

import java.io.Serializable;

@Data
public class DynDiscGrantHistoryId implements Serializable {
    private Integer requestId;
    private Long assignId;
}
