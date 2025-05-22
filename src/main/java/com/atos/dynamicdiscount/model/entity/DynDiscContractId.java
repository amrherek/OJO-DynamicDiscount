package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynDiscContractId implements Serializable {
	private Integer requestId;
	private Integer customerId;
	private Integer coId;

}
