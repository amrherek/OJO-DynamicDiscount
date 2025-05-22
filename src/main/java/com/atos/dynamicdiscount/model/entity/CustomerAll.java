package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "customer_All") 
@Data
public class CustomerAll {

    @Id
    @Column(name = "customer_id") 
    private Integer customerId; 

    @Column(name = "lbc_Date") 
    private LocalDateTime lbcDate;

}