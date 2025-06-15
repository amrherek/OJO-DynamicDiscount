package com.atos.dynamicdiscount.model.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "DYN_DISC_CONTRACT")
@IdClass(DynDiscContractId.class) // Use @IdClass for composite key
@AllArgsConstructor
@NoArgsConstructor
public class DynDiscContract {

	@Id
	@Column(name = "request_id")
	private Integer requestId;
	
	@Id
    @Column(name = "pack_id", nullable = false)
    private Integer packId;

	@Id
	@Column(name = "customer_id")
	private Integer customerId;

	@Id
	@Column(name = "co_id")
	private Integer coId;

	@Column(name = "lbc_date")
	private Date lbcDate;

	@Column(name = "prgcode")
	private String prgcode;

	@Column(name = "tmcode")
	private Integer tmcode;

	@Column(name = "status", nullable = false, length = 1)
	private String status;

	@Column(name = "remark", length = 255)
	private String remark;

	@PrePersist
	@PreUpdate
	private void validateStatus() {
		if (!"IPFS".contains(status)) {
			throw new IllegalArgumentException("Invalid status value: " + status);
		}
	}
}
