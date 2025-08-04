package com.atos.dynamicdiscount.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_PROCESS")
@Data
@NoArgsConstructor

public class DynDiscProcess {

	@Id	
	@Column(name = "PROCESS_ID")
	private Integer processId;


	@Column(name = "UPDATED_AT", nullable = false)
	private LocalDateTime updatedAt;
	
	@Column(name = "COMPONENT", length = 20)
	private String component;
}
