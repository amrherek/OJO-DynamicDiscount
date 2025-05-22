package com.atos.dynamicdiscount.enums;

public enum GmdAction {
	ACTIVATE_CONTRACT(1), DEACTIVATE_CONTRACT(2), REACTIVATE_CONTRACT(3), SUSPENDE_CONTRACT(4),
	DEACTIVATE_SUSPENED_CONTRACT(5), ASSIGN_SERVICE(8), REMOVE_SERVICE(9);

	private final Integer action;

	GmdAction(int action) {
		this.action = action;
	}

	public Integer getAction() {
		return action;
	}
}
