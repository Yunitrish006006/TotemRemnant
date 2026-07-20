package com.adaptor.deadrecall.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Owns unassigned legacy container client registration until it has an approved module.
 */
public final class LegacyContainerClientBootstrap {
    private LegacyContainerClientBootstrap() {
    }

    public static KeyMapping createKeyMapping(KeyMapping.Category category) {
        return new KeyMapping(
                "key.deadrecall.sort_backpack",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                category
        );
    }
}
