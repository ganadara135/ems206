package io.openems.edge.ess.fenecon.commercial40.surplusfeedin;

import io.openems.common.types.OptionsEnum;

enum StateMachine implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DEACTIVATED(0, "Deactivated"), //
	ACTIVATED(1, "Activated"), //
	GOING_DEACTIVATED(2, "Going Deactivated"), //
	PASSED_OFF_TIME(3, "Passed Off-Time"); //

	private final int value;
	private final String name;

	private StateMachine(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}