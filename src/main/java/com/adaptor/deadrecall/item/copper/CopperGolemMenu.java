package com.adaptor.deadrecall.item.copper;

import com.adaptor.deadrecall.menu.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CopperGolemMenu extends AbstractContainerMenu {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_GATHERING_TOOL = 1;
    public static final int SLOT_GATHERING_STORAGE = 2;
    public static final int GOLEM_SLOT_COUNT = 3;

    public static final int FUEL_SLOT_X = 286;
    public static final int FUEL_SLOT_Y = 26;
    public static final int GATHERING_TOOL_SLOT_X = 236;
    public static final int GATHERING_STORAGE_SLOT_X = 282;
    public static final int GATHERING_SLOT_Y = 74;
    public static final int PLAYER_INVENTORY_X = 342;
    public static final int PLAYER_INVENTORY_Y = 146;
    public static final int PLAYER_HOTBAR_Y = 204;

    private static final int PLAYER_INVENTORY_START = GOLEM_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final UUID golemId;
    private final CopperGolem golem;
    private final ServerLevel serverLevel;
    private boolean gatheringSlotsVisible = false;

    public CopperGolemMenu(int containerId, Inventory playerInventory, OpenData data) {
        this(containerId, playerInventory, null, data.golemId(), null, new SimpleContainer(GOLEM_SLOT_COUNT));
    }

    public CopperGolemMenu(int containerId, Inventory playerInventory, Player player, CopperGolem golem) {
        this(
                containerId,
                playerInventory,
                golem,
                golem.getUUID(),
                golem.level() instanceof ServerLevel serverLevel ? serverLevel : null,
                new GolemSlotContainer(golem, player instanceof ServerPlayer serverPlayer ? serverPlayer : null)
        );
    }

    private CopperGolemMenu(
            int containerId,
            Inventory playerInventory,
            CopperGolem golem,
            UUID golemId,
            ServerLevel serverLevel,
            Container golemSlots) {
        super(ModMenus.COPPER_GOLEM, containerId);
        checkContainerSize(golemSlots, GOLEM_SLOT_COUNT);
        this.playerInventory = playerInventory;
        this.golem = golem;
        this.golemId = golemId;
        this.serverLevel = serverLevel;

        addSlot(new FuelSlot(golemSlots, SLOT_FUEL, FUEL_SLOT_X, FUEL_SLOT_Y));
        addSlot(new GatheringToolSlot(golemSlots, SLOT_GATHERING_TOOL, GATHERING_TOOL_SLOT_X, GATHERING_SLOT_Y));
        addSlot(new GatheringStorageSlot(golemSlots, SLOT_GATHERING_STORAGE, GATHERING_STORAGE_SLOT_X, GATHERING_SLOT_Y));
        addPlayerInventorySlots(playerInventory);
    }

    public UUID golemId() {
        return this.golemId;
    }

    public void setGatheringSlotsVisible(boolean visible) {
        this.gatheringSlotsVisible = visible;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < GOLEM_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            Slot toolSlot = this.slots.get(SLOT_GATHERING_TOOL);
            Slot fuelSlot = this.slots.get(SLOT_FUEL);
            boolean movedToGolem = false;
            if (toolSlot.mayPlace(stack) && !toolSlot.hasItem()) {
                movedToGolem = moveItemStackTo(stack, SLOT_GATHERING_TOOL, SLOT_GATHERING_TOOL + 1, false);
            }
            if (!movedToGolem && fuelSlot.mayPlace(stack)) {
                movedToGolem = moveItemStackTo(stack, SLOT_FUEL, SLOT_FUEL + 1, false);
            }
            if (!movedToGolem) {
                if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
                    if (!moveItemStackTo(stack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= PLAYER_HOTBAR_START && index < PLAYER_HOTBAR_END) {
                    if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.golem == null || CopperGolemWrenchHandler.canUseMenu(player, this.golemId, this.golem);
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y));
        }
    }

    private boolean isFuel(ItemStack stack) {
        if (this.serverLevel != null) {
            return CopperGolemWrenchHandler.isFuelForMenu(this.serverLevel, stack);
        }
        return !stack.isEmpty() && this.playerInventory.player.level().fuelValues().isFuel(stack);
    }

    private boolean canEditGatheringSlots() {
        return this.golem == null || CopperGolemWrenchHandler.canEditGatheringSlots(this.golem);
    }

    private boolean areGatheringSlotsVisible() {
        return this.golem == null
                ? this.gatheringSlotsVisible
                : CopperGolemWrenchHandler.getMode(this.golem) == CopperGolemMode.GATHERING;
    }

    public record OpenData(UUID golemId) {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.of(
                (buf, data) -> buf.writeUUID(data.golemId()),
                buf -> new OpenData(buf.readUUID())
        );
    }

    private final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }

    private final class GatheringToolSlot extends Slot {
        private GatheringToolSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canEditGatheringSlots() && CopperGolemWrenchHandler.isGatheringToolForMenu(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return canEditGatheringSlots();
        }

        @Override
        public boolean isActive() {
            return areGatheringSlotsVisible();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private final class GatheringStorageSlot extends Slot {
        private GatheringStorageSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return canEditGatheringSlots();
        }

        @Override
        public boolean isActive() {
            return areGatheringSlotsVisible();
        }

        @Override
        public int getMaxStackSize() {
            return CopperGolemWrenchHandler.transportStorageMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return CopperGolemWrenchHandler.transportStorageMaxStackSize();
        }
    }

    private static final class GolemSlotContainer implements Container {
        private final CopperGolem golem;
        private final ServerPlayer viewer;

        private GolemSlotContainer(CopperGolem golem, ServerPlayer viewer) {
            this.golem = golem;
            this.viewer = viewer;
        }

        @Override
        public int getContainerSize() {
            return GOLEM_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < GOLEM_SLOT_COUNT; slot++) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return switch (slot) {
                case SLOT_FUEL -> CopperGolemWrenchHandler.getFuelStackForMenu(this.golem);
                case SLOT_GATHERING_TOOL -> CopperGolemWrenchHandler.getGatheringToolStackForMenu(this.golem);
                case SLOT_GATHERING_STORAGE -> CopperGolemWrenchHandler.getGatheringStorageStackForMenu(this.golem);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            ItemStack removed = stack.split(amount);
            setItem(slot, stack);
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            switch (slot) {
                case SLOT_FUEL -> CopperGolemWrenchHandler.setFuelStackFromMenu(this.golem, stack);
                case SLOT_GATHERING_TOOL -> CopperGolemWrenchHandler.setGatheringToolStackFromMenu(this.golem, stack);
                case SLOT_GATHERING_STORAGE -> CopperGolemWrenchHandler.setGatheringStorageStackFromMenu(this.golem, stack);
                default -> {
                }
            }
            setChanged();
        }

        @Override
        public void setChanged() {
            if (this.viewer != null) {
                CopperGolemWrenchHandler.refreshMenuPayload(this.viewer, this.golem);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return CopperGolemWrenchHandler.canUseMenu(player, this.golem.getUUID(), this.golem);
        }

        @Override
        public void clearContent() {
            setItem(SLOT_FUEL, ItemStack.EMPTY);
            setItem(SLOT_GATHERING_TOOL, ItemStack.EMPTY);
            setItem(SLOT_GATHERING_STORAGE, ItemStack.EMPTY);
        }
    }
}
