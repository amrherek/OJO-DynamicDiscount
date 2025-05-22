package com.atos.dynamicdiscount.enums;

import lombok.Getter;

@Getter
public enum BillCycle {
    CYCLE_90("90"),
    CYCLE_05("05"),
    CYCLE_02("02"),
    CYCLE_03("03");

    private final String value;

    BillCycle(String value) {
        this.value = value;
    }

    public static BillCycle fromString(String cycleValue) {
        for (BillCycle cycle : BillCycle.values()) {
            if (cycle.getValue().equals(cycleValue)) {
                return cycle;
            }
        }
        throw new IllegalArgumentException("Invalid bill cycle: " + cycleValue + ". Valid values are 90, 05, 02, or 03.");
    }
}
