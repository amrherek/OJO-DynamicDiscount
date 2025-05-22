package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "DYN_DISC_PROCESS")
@Data
@NoArgsConstructor
@ToString

public class DynDiscProcess {

	
	@Column(name = "PROCESS_ID")
	private Integer processId;

	@Id
	@Column(name = "LAST_REQ_ID", nullable = false)
	private Long lastReqId;

	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;
	
	@Column(name = "COMPONENT", length = 20)
	private String component;
}
