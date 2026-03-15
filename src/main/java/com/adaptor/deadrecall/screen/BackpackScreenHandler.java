package com.adaptor.deadrecall.screen;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.DeathBackpackItem;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class BackpackScreenHandler extends ScreenHandler {
    private static final int MAX_VISIBLE_ROWS = 6;

    private final Inventory inventory;
    private final int totalRows;
    private final int backpackSlots;
    private final int maxVisibleRows;
    private int scrollRow = 0;

    public static final ExtendedScreenHandlerType<BackpackScreenHandler, Integer> SCREEN_HANDLER_TYPE =
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of("deadrecall", "backpack"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> {
                    int rows = Math.max(1, data);
                    return new BackpackScreenHandler(syncId, playerInventory, rows);
                },
                PacketCodecs.INTEGER
            ));

    // 客戶端構造函數（從網路數據讀取行數）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int rows) {
        this(syncId, playerInventory, new SimpleInventory(rows * 9), rows);
    }

    // 伺服器端構造函數（普通背包，使用等級）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PlayerEntity player, Hand hand, TieredBackpackItem.BackpackTier tier) {
        this(syncId, playerInventory, new BackpackInventory(player, hand, tier), tier.getRows());
    }

    // 伺服器端構造函數（死亡背包，動態行數）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PlayerEntity player, Hand hand, int rows) {
        this(syncId, playerInventory, new BackpackInventory(player, hand, rows * 9), rows);
    }

    // 主要構造函數
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int rows) {
        super(SCREEN_HANDLER_TYPE, syncId);
        this.inventory = inventory;
        this.totalRows = rows;
        this.backpackSlots = rows * 9;
        this.maxVisibleRows = Math.min(rows, MAX_VISIBLE_ROWS);

        checkSize(inventory, backpackSlots);
        inventory.onOpen(playerInventory.player);

        // 背包槽位 - 根據行數動態生成
        for (int row = 0; row < rows; row++) {
            final int rowIndex = row;
            for (int col = 0; col < 9; col++) {
                int slotY = row < maxVisibleRows ? (18 + row * 18) : -999;
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, slotY) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        if (inventory instanceof BackpackInventory backpackInv) {
                            ItemStack backpackStack = backpackInv.getBackpackStack();
                            if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof DeathBackpackItem) {
                                return false;
                            }
                        }
                        return !(stack.getItem() instanceof TieredBackpackItem);
                    }

                    @Override
                    public boolean isEnabled() {
                        return rowIndex >= scrollRow && rowIndex < scrollRow + maxVisibleRows;
                    }
                });
            }
        }

        // 玩家背包位置需要根據可見行數調整
        int playerInventoryY = 18 + maxVisibleRows * 18 + 14;

        // 玩家背包（3行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        // 玩家快捷欄（1行）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInventoryY + 58));
        }
    }

    // ===== 滾動相關方法 =====

    public int getRows() {
        return totalRows;
    }

    public int getMaxVisibleRows() {
        return maxVisibleRows;
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public int getMaxScrollRow() {
        return Math.max(0, totalRows - maxVisibleRows);
    }

    public boolean canScroll() {
        return totalRows > maxVisibleRows;
    }

    public void setScrollRow(int row) {
        this.scrollRow = Math.max(0, Math.min(row, getMaxScrollRow()));
        updateSlotPositions();
    }

    private void updateSlotPositions() {
        for (int i = 0; i < backpackSlots; i++) {
            Slot slot = this.slots.get(i);
            int row = i / 9;
            if (row >= scrollRow && row < scrollRow + maxVisibleRows) {
                slot.y = 18 + (row - scrollRow) * 18;
            } else {
                slot.y = -999;
            }
        }
    }

    // ===== ScreenHandler 方法 =====

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj != null && slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            boolean isDeathBackpack = false;
            if (inventory instanceof BackpackInventory backpackInv) {
                ItemStack backpackStack = backpackInv.getBackpackStack();
                if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof DeathBackpackItem) {
                    isDeathBackpack = true;
                }
            }

            if (slot < backpackSlots) {
                // 從背包移動到玩家背包 - 總是允許（包括死亡背包裡的背包物品）
                if (!this.insertItem(originalStack, backpackSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包移動到背包 - 死亡背包不允許
                if (isDeathBackpack) {
                    return ItemStack.EMPTY;
                }
                // 防止背包套娃（只在放入背包時檢查，取出時不限制）
                if (originalStack.getItem() instanceof TieredBackpackItem) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(originalStack, 0, backpackSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
