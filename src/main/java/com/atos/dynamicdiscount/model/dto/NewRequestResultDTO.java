package com.atos.dynamicdiscount.model.dto;

import java.util.List;

import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewRequestResultDTO {
	private DynDiscRequest request;
	private List<DynDiscContract> dynDiscContracts;
}