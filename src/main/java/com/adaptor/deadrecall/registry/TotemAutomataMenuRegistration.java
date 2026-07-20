package com.adaptor.deadrecall.registry;

import com.adaptor.deadrecall.item.copper.CopperGolemMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class TotemAutomataMenuRegistration {
    public static final ExtendedMenuType<CopperGolemMenu, CopperGolemMenu.OpenData> COPPER_GOLEM =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath("deadrecall", "copper_golem"),
                    new ExtendedMenuType<>(CopperGolemMenu::new, CopperGolemMenu.OpenData.STREAM_CODEC)
            );

    private TotemAutomataMenuRegistration() {
    }

    public static void register() {
        // Class loading registers this owner's menu types.
    }
}
