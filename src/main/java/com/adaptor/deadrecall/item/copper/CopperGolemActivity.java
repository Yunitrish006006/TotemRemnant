package com.adaptor.deadrecall.item.copper;

import java.util.Locale;

public enum CopperGolemActivity {
    STOPPED("stopped"),
    IDLE("idle"),
    SEARCHING("searching"),
    MOVING_TO_TARGET("moving_to_target"),
    WORKING("working"),
    RETURNING_HOME("returning_home"),
    DEPOSITING("depositing"),
    BLOCKED_NO_FUEL("blocked_no_fuel"),
    BLOCKED_SORTING("blocked_sorting"),
    BLOCKED_NO_TOOL("blocked_no_tool"),
    BLOCKED_TOOL_BROKEN("blocked_tool_broken"),
    BLOCKED_NO_AREA("blocked_no_area"),
    BLOCKED_NO_HOME("blocked_no_home"),
    BLOCKED_HOME_UNAVAILABLE("blocked_home_unavailable"),
    BLOCKED_HOME_FULL("blocked_home_full"),
    BLOCKED_NO_VALID_TARGET("blocked_no_valid_target");

    private final String id;

    CopperGolemActivity(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static CopperGolemActivity fromId(String id) {
        if (id == null || id.isBlank()) {
            return IDLE;
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        for (CopperGolemActivity activity : values()) {
            if (activity.id.equals(normalized)) {
                return activity;
            }
        }
        return IDLE;
    }
}
