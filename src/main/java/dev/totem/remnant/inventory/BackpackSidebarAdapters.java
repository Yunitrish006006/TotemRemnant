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
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Inventory;

/** Explicit, immutable registrations. Subclasses never inherit a sidebar adapter. */
public enum BackpackSidebarAdapters {
    INVENTORY(InventoryMenu.class, "inventory", 46),
    CRAFTING(CraftingMenu.class, "crafting", 46),
    ENCHANTING(EnchantmentMenu.class, "enchanting", 38),
    CHEST(ChestMenu.class, "chest", -1),
    FURNACE(FurnaceMenu.class, "furnace", 39),
    SMOKER(SmokerMenu.class, "smoker", 39),
    BLAST_FURNACE(BlastFurnaceMenu.class, "blast_furnace", 39),
    ANVIL(AnvilMenu.class, "anvil", 39),
    STONECUTTER(StonecutterMenu.class, "stonecutter", 38),
    SMITHING(SmithingMenu.class, "smithing", 40),
    GRINDSTONE(GrindstoneMenu.class, "grindstone", 39),
    LOOM(LoomMenu.class, "loom", 40),
    CARTOGRAPHY(CartographyTableMenu.class, "cartography", 39),
    BREWING(BrewingStandMenu.class, "brewing", 41),
    HOPPER(HopperMenu.class, "hopper", 41),
    MERCHANT(MerchantMenu.class, "merchant", 39);

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

    /** Uses the real native client menu constructors for detached Observer reconstruction. */
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return switch (this) {
            case INVENTORY -> inventory.player.inventoryMenu;
            case CRAFTING -> new CraftingMenu(id, inventory);
            case ENCHANTING -> new EnchantmentMenu(id, inventory);
            case CHEST -> ChestMenu.threeRows(id, inventory);
            case FURNACE -> new FurnaceMenu(id, inventory);
            case SMOKER -> new SmokerMenu(id, inventory);
            case BLAST_FURNACE -> new BlastFurnaceMenu(id, inventory);
            case ANVIL -> new AnvilMenu(id, inventory);
            case STONECUTTER -> new StonecutterMenu(id, inventory);
            case SMITHING -> new SmithingMenu(id, inventory);
            case GRINDSTONE -> new GrindstoneMenu(id, inventory);
            case LOOM -> new LoomMenu(id, inventory);
            case CARTOGRAPHY -> new CartographyTableMenu(id, inventory);
            case BREWING -> new BrewingStandMenu(id, inventory);
            case HOPPER -> new HopperMenu(id, inventory);
            case MERCHANT -> new MerchantMenu(id, inventory);
        };
    }

    public static BackpackSidebarAdapters find(AbstractContainerMenu menu) { return BY_MENU.get(menu.getClass()); }
    public static Set<String> variants() { return VARIANTS; }
    public String variant() { return variant; }
    public int nativeSlotCount(AbstractContainerMenu menu) {
        if (menu.getClass() != menuClass) throw new IllegalArgumentException("Mismatched sidebar menu adapter");
        return this == CHEST ? ((ChestMenu) menu).getRowCount() * 9 + 36 : nativeSlots;
    }
}
