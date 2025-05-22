package com.atos.dynamicdiscount.model.dto;

import com.atos.dynamicdiscount.model.entity.DynDiscContract;
import com.atos.dynamicdiscount.model.entity.DynDiscEvalHistory;
import com.atos.dynamicdiscount.model.entity.DynDiscGrantHistory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class DynDiscGrantEvalDTO {
	
	 private DynDiscContract dynDiscContract;
	 private DynDiscEvalHistory dynDiscEvalHistory;
	 private DynDiscGrantHistory dynDiscGrantHistory;
}


