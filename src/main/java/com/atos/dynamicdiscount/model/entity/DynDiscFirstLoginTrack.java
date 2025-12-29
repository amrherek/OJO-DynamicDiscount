package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "DYN_DISC_FIRST_LOGIN_TRACK")
@Data
public class DynDiscFirstLoginTrack {

    @Id
    @Column(name = "CO_ID")
    private Integer coId;
    
    @Column(name = "CUSTOMER_ID")
    private Integer customerId;

    @Column(name = "TMCODE")
    private Integer tmcode;
    
    @Column(name = "FIRST_LOGIN")
    private LocalDateTime firstLogin;

    @Column(name = "PROCESSED_FLG")
    private String processedFlg = "N";

    @Column(name = "PROCESSED_DATE")
    private LocalDateTime processedDate;

    @Column(name = "COMMENTS")
    private String comments;
}
