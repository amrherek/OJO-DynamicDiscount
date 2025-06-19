package com.atos.dynamicdiscount.model.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "GMD_REQUEST_HISTORY")
@Data 
@NoArgsConstructor 

public class GmdRequestHistory {

    @Id
    @Column(name = "REQUEST")
    private Long request;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private Long customerId;

    @Column(name = "CO_ID")
    private Long coId;

    @Column(name = "ACTION_ID", nullable = false)
    private Integer actionId;
}



