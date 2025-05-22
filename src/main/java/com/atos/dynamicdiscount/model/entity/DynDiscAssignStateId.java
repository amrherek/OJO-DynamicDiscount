package com.atos.dynamicdiscount.model.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DynDiscAssignStateId implements Serializable {
	private Long assignId;
	private Integer seqno;
}