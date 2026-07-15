package com.adaptor.deadrecall.menu;

import com.adaptor.deadrecall.item.copper.CopperGolemMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModMenus {
    public static final ExtendedMenuType<CopperGolemMenu, CopperGolemMenu.OpenData> COPPER_GOLEM =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath("deadrecall", "copper_golem"),
                    new ExtendedMenuType<>(CopperGolemMenu::new, CopperGolemMenu.OpenData.STREAM_CODEC)
            );

    private ModMenus() {
    }

    public static void registerModMenus() {
        // Class loading registers menu types.
    }
}
