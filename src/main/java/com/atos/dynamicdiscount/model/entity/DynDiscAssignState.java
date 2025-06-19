package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_ASSIGN_STATE")
@IdClass(DynDiscAssignStateId.class)  
@Data  
@NoArgsConstructor  
public class DynDiscAssignState {

    @Id
    @Column(name = "assign_id")
    private Long assignId;

    @Id
    @Column(name = "seqno")
    private Integer seqno;

    @Column(name = "action_id")
    private Integer actionId;

    @Column(name = "status")
    private String status;

    @Column(name = "status_date")
    private LocalDateTime statusDate;
    
	@Column(name = "gmd_request")
	private Integer gmdRequest;
	
	@Column(name = "entry_date")
	private LocalDateTime entryDate;
}
