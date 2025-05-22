package com.atos.dynamicdiscount.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_TYPE")
@Data
@NoArgsConstructor
public class DynDiscType {

    @Id
    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @Column(name = "description", nullable = false, length = 200)
    private String description;
}
