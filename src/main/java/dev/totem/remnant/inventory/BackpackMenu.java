package dev.totem.remnant.inventory;

import dev.totem.remnant.registry.BackpackMenuRegistration;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.upgrade.BackpackUpgradeItem;
import dev.totem.remnant.upgrade.BackpackUpgradeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** Backpack storage plus dedicated removable upgrade slots and an embedded 3x3 crafting grid. */
public final class BackpackMenu extends ChestMenu {
    public static final int ENDER_ACCESS_BUTTON_ID = 1;
    public static final int UPGRADE_PANEL_X = 177;
    public static final int UPGRADE_PANEL_WIDTH = 102;
    public static final int CRAFTING_PANEL_X = 177;
    public static final int CRAFTING_PANEL_Y = 41;
    public static final int CRAFTING_PANEL_WIDTH = 102;
    public static final int CRAFTING_GRID_X = 184;
    public static final int CRAFTING_GRID_Y = 58;
    public static final int CRAFTING_RESULT_X = 257;
    public static final int CRAFTING_RESULT_Y = 76;
    private static final long REJECTION_MESSAGE_COOLDOWN_TICKS = 20L;

    private final ItemStack trackedBackpackStack;
    private final BackpackUpgradeInventory upgrades;
    private final Player owner;
    private final CraftingContainer craftingSlots;
    private final ResultContainer craftingResult = new ResultContainer();
    private final int backpackSlotCount;
    private final int baseBackpackSlotCount;
    private final int playerSlotStart;
    private final int upgradeSlotStart;
    private final int craftingResultSlotIndex;
    private final int craftingInputSlotStart;
    private final int craftingInputSlotEnd;
    private boolean craftingEnabled;
    private long nextRejectionMessageGameTime;

    private BackpackMenu(
            MenuType<?> menuType,
            int containerId,
            Inventory playerInventory,
            net.minecraft.world.Container backpackInventory,
            BackpackUpgradeInventory upgrades,
            int rows,
            ItemStack trackedBackpackStack
    ) {
        super(menuType, containerId, playerInventory, backpackInventory, rows);
        this.trackedBackpackStack = trackedBackpackStack;
        this.upgrades = upgrades;
        this.owner = playerInventory.player;
        this.craftingSlots = new TransientCraftingContainer(this, 3, 3);
        this.backpackSlotCount = rows * 9;
        this.baseBackpackSlotCount = trackedBackpackStack.getItem() instanceof TieredBackpackItem tiered
                ? tiered.tier().slots()
                : backpackSlotCount;
        this.playerSlotStart = backpackSlotCount;
        this.upgradeSlotStart = playerSlotStart + 36;
        int upgradeSlotStartX = UPGRADE_PANEL_X
                + (UPGRADE_PANEL_WIDTH - upgrades.getContainerSize() * 18) / 2;
        for (int index = 0; index < upgrades.getContainerSize(); index++) {
            int x = upgradeSlotStartX + index * 18;
            int y = 17;
            addSlot(new UpgradeSlot(upgrades, index, x, y));
        }
        if (upgrades.getContainerSize() > 0) {
            this.craftingResultSlotIndex = slots.size();
            addSlot(new EmbeddedResultSlot(owner, craftingSlots, craftingResult,
                    CRAFTING_RESULT_X, CRAFTING_RESULT_Y));
            this.craftingInputSlotStart = slots.size();
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int index = column + row * 3;
                    addSlot(new EmbeddedCraftingSlot(craftingSlots, index,
                            CRAFTING_GRID_X + column * 18, CRAFTING_GRID_Y + row * 18));
                }
            }
            this.craftingInputSlotEnd = slots.size();
        } else {
            // Death Backpacks retain vanilla generic-menu slot counts and screens.
            this.craftingResultSlotIndex = -1;
            this.craftingInputSlotStart = slots.size();
            this.craftingInputSlotEnd = slots.size();
        }
        this.craftingEnabled = hasUpgrade(BackpackUpgradeType.CRAFTING);
    }

    public static BackpackMenu serverSide(int containerId, Inventory playerInventory,
                                          DeathBackpackInventory backpackInventory,
                                          BackpackUpgradeInventory upgrades, int rows) {
        return new BackpackMenu(BackpackMenuRegistration.BACKPACK, containerId, playerInventory, backpackInventory, upgrades, rows,
                backpackInventory.getBackpackStack());
    }

    public static BackpackMenu clientSide(int containerId, Inventory playerInventory,
                                          int rows, int upgradeSlots) {
        return new BackpackMenu(BackpackMenuRegistration.BACKPACK, containerId, playerInventory,
                new SimpleContainer(rows * 9), BackpackUpgradeInventory.clientSide(upgradeSlots),
                rows, ItemStack.EMPTY);
    }

    /** Preserved generic menu path for non-upgradeable death backpacks. */
    public BackpackMenu(MenuType<?> menuType, int containerId, Inventory playerInventory,
                        DeathBackpackInventory backpackInventory, int rows) {
        this(menuType, containerId, playerInventory, backpackInventory,
                BackpackUpgradeInventory.clientSide(0), rows, backpackInventory.getBackpackStack());
    }

    public int upgradeSlotCount() {
        return upgrades.getContainerSize();
    }

    public boolean hasUpgrade(BackpackUpgradeType type) {
        for (int index = 0; index < upgrades.getContainerSize(); index++) {
            if (upgrades.getItem(index).getItem() instanceof BackpackUpgradeItem module
                    && module.type() == type) return true;
        }
        return false;
    }

    public boolean isCraftingEnabled() {
        return craftingResultSlotIndex >= 0 && hasUpgrade(BackpackUpgradeType.CRAFTING);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != ENDER_ACCESS_BUTTON_ID
                || !(player instanceof ServerPlayer serverPlayer)
                || player != owner
                || player.containerMenu != this
                || trackedBackpackStack.isEmpty()
                || !upgrades.stillValid(player)
                || !hasUpgrade(BackpackUpgradeType.ENDER_ACCESS)) {
            return false;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> ChestMenu.threeRows(
                        containerId, inventory, serverPlayer.getEnderChestInventory()),
                Component.translatable("container.enderchest")
        ));
        return true;
    }

    public Slot craftingResultSlot() {
        if (craftingResultSlotIndex < 0) {
            throw new IllegalStateException("This backpack menu has no crafting expansion");
        }
        return slots.get(craftingResultSlotIndex);
    }

    public List<Slot> craftingInputSlots() {
        return List.copyOf(slots.subList(craftingInputSlotStart, craftingInputSlotEnd));
    }

    public int craftingResultSlotIndex() {
        return craftingResultSlotIndex;
    }

    public int craftingInputSlotStart() {
        return craftingInputSlotStart;
    }

    public int backpackSlotCount() {
        return backpackSlotCount;
    }

    public int playerSlotStart() {
        return playerSlotStart;
    }

    public int upgradeSlotStart() {
        return upgradeSlotStart;
    }

    /**
     * Repositions only the client-side visual slot objects. Menu indices and backing
     * container indices stay unchanged, so normal vanilla click packets continue to
     * address the correct real backpack slots without a page-sync protocol.
     */
    public void applyClientScrollLayout(int firstVisibleRow, int visibleRows) {
        if (!owner.level().isClientSide()) return;
        int firstRow = Math.max(0, Math.min(firstVisibleRow, getRowCount() - visibleRows));
        for (int menuIndex = 0; menuIndex < backpackSlotCount; menuIndex++) {
            Slot original = slots.get(menuIndex);
            int storageRow = menuIndex / 9;
            boolean visible = storageRow >= firstRow && storageRow < firstRow + visibleRows;
            int x = 8 + menuIndex % 9 * 18;
            int y = 18 + (storageRow - firstRow) * 18;
            replaceClientSlot(menuIndex, original, x, y, visible);
        }
        for (int menuIndex = playerSlotStart; menuIndex < upgradeSlotStart; menuIndex++) {
            Slot original = slots.get(menuIndex);
            int playerIndex = menuIndex - playerSlotStart;
            int x;
            int y;
            if (playerIndex < 27) {
                x = 8 + playerIndex % 9 * 18;
                y = visibleRows * 18 + 31 + playerIndex / 9 * 18;
            } else {
                x = 8 + (playerIndex - 27) * 18;
                y = visibleRows * 18 + 89;
            }
            replaceClientSlot(menuIndex, original, x, y, true);
        }
    }

    private void replaceClientSlot(int menuIndex, Slot original, int x, int y, boolean active) {
        ClientLayoutSlot replacement = new ClientLayoutSlot(
                original.container, original.getContainerSlot(), x, y, active);
        replacement.index = menuIndex;
        slots.set(menuIndex, replacement);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == craftingSlots) updateCraftingResult();
    }

    @Override
    public void broadcastChanges() {
        boolean enabled = isCraftingEnabled();
        if (craftingResultSlotIndex >= 0 && enabled != craftingEnabled) {
            craftingEnabled = enabled;
            updateCraftingResult();
        }
        super.broadcastChanges();
    }

    private void updateCraftingResult() {
        if (!(owner instanceof ServerPlayer serverPlayer)
                || !(owner.level() instanceof ServerLevel serverLevel)) return;

        ItemStack output = ItemStack.EMPTY;
        if (isCraftingEnabled()) {
            CraftingInput input = craftingSlots.asCraftInput();
            RecipeHolder<CraftingRecipe> recipe = serverLevel.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, input, serverLevel)
                    .orElse(null);
            if (recipe != null && craftingResult.setRecipeUsed(serverPlayer, recipe)) {
                ItemStack assembled = recipe.value().assemble(input);
                if (assembled.isItemEnabled(serverLevel.enabledFeatures())) output = assembled;
            }
        }

        craftingResult.setItem(0, output);
        setRemoteSlot(craftingResultSlotIndex, output);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), craftingResultSlotIndex, output));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(slotIndex);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = source.getItem();
        ItemStack original = sourceStack.copy();

        boolean moved;
        if (slotIndex == craftingResultSlotIndex) {
            if (!isCraftingEnabled()) return ItemStack.EMPTY;
            sourceStack.getItem().onCraftedBy(sourceStack, player);
            moved = moveItemStackTo(sourceStack, playerSlotStart, upgradeSlotStart, true);
            if (moved) source.onQuickCraft(sourceStack, original);
        } else if (slotIndex >= craftingInputSlotStart && slotIndex < craftingInputSlotEnd) {
            if (!isCraftingEnabled()) return ItemStack.EMPTY;
            moved = moveItemStackTo(sourceStack, playerSlotStart, upgradeSlotStart, true);
        } else if (slotIndex >= upgradeSlotStart && slotIndex < craftingResultSlotIndex) {
            if (!mayRemoveUpgrade(sourceStack)) return ItemStack.EMPTY;
            moved = moveItemStackTo(sourceStack, playerSlotStart, upgradeSlotStart, true);
        } else if (slotIndex < backpackSlotCount) {
            moved = moveItemStackTo(sourceStack, playerSlotStart, upgradeSlotStart, true);
        } else if (craftingResultSlotIndex >= 0
                && sourceStack.getItem() instanceof BackpackUpgradeItem) {
            moved = moveItemStackTo(sourceStack, upgradeSlotStart, craftingResultSlotIndex, false);
        } else if (!mayInsertIntoBackpack(sourceStack)) {
            notifyRestrictedInsertion(player);
            return ItemStack.EMPTY;
        } else {
            moved = moveItemStackTo(sourceStack, 0, activeBackpackSlotCount(), false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (sourceStack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, sourceStack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, craftingSlots);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (!isCraftingEnabled() && (slotIndex == craftingResultSlotIndex
                || slotIndex >= craftingInputSlotStart && slotIndex < craftingInputSlotEnd)) {
            return;
        }
        if (targetsTrackedBackpack(slotIndex)
                || swapsTrackedBackpackFromInventory(player.getInventory(), input, buttonNum)) return;
        if (insertsIntoInactiveCapacitySlot(slotIndex, input, player.getInventory(), buttonNum)) return;
        if (collectsRestrictedContainersWithPickupAll(input)
                || insertsRestrictedCarriedStack(slotIndex, input)
                || swapsRestrictedInventoryStackIntoBackpack(slotIndex, player.getInventory(), input, buttonNum)) {
            notifyRestrictedInsertion(player);
            return;
        }
        super.clicked(slotIndex, buttonNum, input, player);
    }

    private boolean targetsTrackedBackpack(int slotIndex) {
        return !trackedBackpackStack.isEmpty() && slotIndex >= 0 && slotIndex < slots.size()
                && slots.get(slotIndex).getItem() == trackedBackpackStack;
    }

    private boolean swapsTrackedBackpackFromInventory(Inventory inventory, ContainerInput input, int inventorySlot) {
        return !trackedBackpackStack.isEmpty() && input == ContainerInput.SWAP
                && inventorySlot >= 0 && inventorySlot < inventory.getContainerSize()
                && inventory.getItem(inventorySlot) == trackedBackpackStack;
    }

    private boolean collectsRestrictedContainersWithPickupAll(ContainerInput input) {
        return input == ContainerInput.PICKUP_ALL
                && PortableContainerPolicy.isRestrictedPortableContainer(getCarried());
    }

    private boolean insertsRestrictedCarriedStack(int slotIndex, ContainerInput input) {
        return slotIndex >= 0 && slotIndex < backpackSlotCount
                && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_CRAFT)
                && !mayInsertIntoBackpack(getCarried());
    }

    private boolean swapsRestrictedInventoryStackIntoBackpack(int slotIndex, Inventory inventory,
                                                               ContainerInput input, int inventorySlot) {
        return slotIndex >= 0 && slotIndex < backpackSlotCount && input == ContainerInput.SWAP
                && inventorySlot >= 0 && inventorySlot < inventory.getContainerSize()
                && !mayInsertIntoBackpack(inventory.getItem(inventorySlot));
    }

    private boolean mayInsertIntoBackpack(ItemStack stack) {
        return owner.level() instanceof ServerLevel serverLevel
                ? PortableContainerPolicy.mayInsertIntoBackpack(serverLevel, stack)
                : PortableContainerPolicy.mayInsertIntoBackpack(stack);
    }

    private void notifyRestrictedInsertion(Player player) {
        if (player.level().isClientSide()) return;
        long gameTime = player.level().getGameTime();
        if (gameTime < nextRejectionMessageGameTime) return;
        nextRejectionMessageGameTime = gameTime + REJECTION_MESSAGE_COOLDOWN_TICKS;
        player.sendSystemMessage(Component.translatable("item.deadrecall.backpack.tooltip.no_nesting"));
    }

    private boolean mayRemoveUpgrade(ItemStack upgrade) {
        if (!(upgrade.getItem() instanceof BackpackUpgradeItem module)) return true;
        return switch (module.type()) {
            case CAPACITY -> capacityRowIsEmpty();
            case CRAFTING -> craftingGridIsEmpty();
            default -> true;
        };
    }

    private boolean craftingGridIsEmpty() {
        for (int index = 0; index < craftingSlots.getContainerSize(); index++) {
            if (!craftingSlots.getItem(index).isEmpty()) return false;
        }
        return true;
    }

    private boolean capacityRowIsEmpty() {
        int activeSlots = activeBackpackSlotCount();
        int removedRowStart = Math.max(baseBackpackSlotCount,
                activeSlots - dev.totem.remnant.upgrade.BackpackCapacity.SLOTS_PER_MODULE);
        for (int index = removedRowStart; index < activeSlots; index++) {
            if (slots.get(index).hasItem()) return false;
        }
        return true;
    }

    private int activeBackpackSlotCount() {
        int capacityModules = 0;
        for (int index = 0; index < upgrades.getContainerSize(); index++) {
            if (upgrades.getItem(index).getItem() instanceof BackpackUpgradeItem module
                    && module.type() == BackpackUpgradeType.CAPACITY) capacityModules++;
        }
        return Math.min(backpackSlotCount, baseBackpackSlotCount
                + capacityModules * dev.totem.remnant.upgrade.BackpackCapacity.SLOTS_PER_MODULE);
    }

    private boolean insertsIntoInactiveCapacitySlot(int slotIndex, ContainerInput input,
                                                     Inventory inventory, int inventorySlot) {
        if (slotIndex < activeBackpackSlotCount() || slotIndex >= backpackSlotCount) return false;
        return (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_CRAFT)
                        && !getCarried().isEmpty()
                || input == ContainerInput.SWAP
                        && inventorySlot >= 0 && inventorySlot < inventory.getContainerSize()
                        && !inventory.getItem(inventorySlot).isEmpty();
    }

    private final class UpgradeSlot extends Slot {
        private UpgradeSlot(BackpackUpgradeInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof BackpackUpgradeItem && container.canPlaceItem(index, stack);
        }

        @Override public boolean mayPickup(Player player) {
            return mayRemoveUpgrade(getItem());
        }

        @Override public int getMaxStackSize() { return 1; }
    }

    private static final class ClientLayoutSlot extends Slot {
        private final boolean active;

        private ClientLayoutSlot(Container container, int index, int x, int y, boolean active) {
            super(container, index, x, y);
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    }

    private final class EmbeddedCraftingSlot extends Slot {
        private EmbeddedCraftingSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isCraftingEnabled();
        }

        @Override public boolean mayPickup(Player player) {
            return isCraftingEnabled();
        }

        @Override public boolean isActive() {
            return isCraftingEnabled();
        }
    }

    private final class EmbeddedResultSlot extends ResultSlot {
        private EmbeddedResultSlot(Player player, CraftingContainer craftingSlots,
                                   Container result, int x, int y) {
            super(player, craftingSlots, result, 0, x, y);
        }

        @Override public boolean mayPickup(Player player) {
            return isCraftingEnabled() && super.mayPickup(player);
        }

        @Override public boolean isActive() {
            return isCraftingEnabled();
        }
    }
}
