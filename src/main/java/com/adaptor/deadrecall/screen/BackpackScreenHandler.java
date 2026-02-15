package com.adaptor.deadrecall.screen;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class BackpackScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public static final ScreenHandlerType<BackpackScreenHandler> SCREEN_HANDLER_TYPE =
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of("deadrecall", "backpack"),
            new ScreenHandlerType<>(BackpackScreenHandler::new, null));

    // 客户端构造函数
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(BackpackInventory.SIZE));
    }

    // 服务器端构造函数（使用玩家和手的引用）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PlayerEntity player, Hand hand) {
        this(syncId, playerInventory, new BackpackInventory(player, hand));
    }

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(SCREEN_HANDLER_TYPE, syncId);
        this.inventory = inventory;
        checkSize(inventory, BackpackInventory.SIZE);
        inventory.onOpen(playerInventory.player);

        // 背包的3行9列
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        // 防止背包套娃
                        return stack.getItem() != ModItems.BACKPACK;
                    }
                });
            }
        }

        // 玩家背包（3行）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷欄（1行）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj != null && slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            // 防止背包套娃
            if (originalStack.getItem() == ModItems.BACKPACK) {
                return ItemStack.EMPTY;
            }

            if (slot < BackpackInventory.SIZE) {
                // 從背包移動到玩家背包
                if (!this.insertItem(originalStack, BackpackInventory.SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包移動到背包
                if (!this.insertItem(originalStack, 0, BackpackInventory.SIZE, false)) {
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



