package dev.totem.remnant.inventory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.ChestMenu;

/** Explicit, immutable registrations. Subclasses never inherit a sidebar adapter. */
public enum BackpackSidebarAdapters {
    INVENTORY(InventoryMenu.class, "inventory", 46),
    CRAFTING(CraftingMenu.class, "crafting", 46),
    ENCHANTING(EnchantmentMenu.class, "enchanting", 38),
    CHEST(ChestMenu.class, "chest", -1);

    private static final Map<Class<?>, BackpackSidebarAdapters> BY_MENU = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(adapter -> adapter.menuClass, Function.identity()));
    private static final Set<String> VARIANTS = Arrays.stream(values())
            .map(BackpackSidebarAdapters::variant).collect(Collectors.toUnmodifiableSet());
    private final Class<? extends AbstractContainerMenu> menuClass;
    private final String variant;
    private final int nativeSlots;

    BackpackSidebarAdapters(Class<? extends AbstractContainerMenu> menuClass, String variant, int nativeSlots) {
        this.menuClass = menuClass;
        this.variant = variant;
        this.nativeSlots = nativeSlots;
    }

    public static BackpackSidebarAdapters find(AbstractContainerMenu menu) { return BY_MENU.get(menu.getClass()); }
    public static Set<String> variants() { return VARIANTS; }
    public String variant() { return variant; }
    public int nativeSlotCount(AbstractContainerMenu menu) {
        if (menu.getClass() != menuClass) throw new IllegalArgumentException("Mismatched sidebar menu adapter");
        return this == CHEST ? ((ChestMenu) menu).getRowCount() * 9 + 36 : nativeSlots;
    }
}
