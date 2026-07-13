package com.adaptor.deadrecall.item.copper;

import java.util.Locale;

public enum CopperGolemMode {
    SORTING("sorting"),
    GATHERING("gathering");

    private final String id;

    CopperGolemMode(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public CopperGolemMode next() {
        return this == SORTING ? GATHERING : SORTING;
    }

    public static CopperGolemMode fromId(String id) {
        if (id == null || id.isBlank()) {
            return SORTING;
        }

        String normalized = id.toLowerCase(Locale.ROOT);
        for (CopperGolemMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return SORTING;
    }
}
