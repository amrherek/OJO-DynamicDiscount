package com.atos.dynamicdiscount.enums;

public enum DynDiscountState {
	ACTIVE("A"), SUSPENDED("S"), DEACTIVE("D");

	private final String code;

	DynDiscountState(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
