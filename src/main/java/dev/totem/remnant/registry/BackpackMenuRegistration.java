package dev.totem.remnant.registry;

import dev.totem.remnant.inventory.BackpackMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/** One extended menu type carries storage rows and upgrade capacity to the client. */
public final class BackpackMenuRegistration {
    public static final ExtendedMenuType<BackpackMenu, Integer> BACKPACK = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath("totem", "remnant/backpack"),
            new ExtendedMenuType<>(
                    (containerId, inventory, data) -> BackpackMenu.clientSide(
                            containerId, inventory, data & 0xFF, data >>> 8 & 0xFF),
                    ByteBufCodecs.VAR_INT
            )
    );

    private BackpackMenuRegistration() {
    }

    public static void register() {
        // Class initialization performs registration.
    }

    public static int openingData(int rows, int upgradeSlots) {
        return rows & 0xFF | (upgradeSlots & 0xFF) << 8;
    }
}
