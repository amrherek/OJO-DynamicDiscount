package com.atos.dynamicdiscount.model.entity;
import java.io.Serializable;

import lombok.Data;

@Data
public class DynDiscPackageId implements Serializable {
    private Integer requestId;
    private Integer packId;
}
