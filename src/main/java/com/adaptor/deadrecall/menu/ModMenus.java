package com.adaptor.deadrecall.menu;

import com.adaptor.deadrecall.registry.TotemAutomataMenuRegistration;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import com.adaptor.deadrecall.item.copper.CopperGolemMenu;

public final class ModMenus {
    public static final ExtendedMenuType<CopperGolemMenu, CopperGolemMenu.OpenData> COPPER_GOLEM =
            TotemAutomataMenuRegistration.COPPER_GOLEM;

    private ModMenus() {
    }

    public static void registerModMenus() {
        // Class loading registers menu types.
    }
}
