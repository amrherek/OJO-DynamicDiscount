package com.atos.dynamicdiscount.model.entity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DYN_DISC_PACKAGE")
@Data
@IdClass(DynDiscPackageId.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynDiscPackage {

    @Id
    @Column(name = "request_id", nullable = false)
    private Integer requestId;

    @Id
    @Column(name = "pack_id", nullable = false)
    private Integer packId;

    @Column(name = "status", nullable = false, length = 1)
    private String status;

    @Column(name = "entry_Date", nullable = false)
    private LocalDateTime entryDate;

    @Column(name = "start_Date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "contract_count", nullable = false)
    private Integer contractCount;
    
    
	@PrePersist
	@PreUpdate
	private void validateStatus() {
		if (!"IPFW".contains(status)) {
			throw new IllegalArgumentException("Invalid status value: " + status);
		}
	}

}
