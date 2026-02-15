package com.adaptor.deadrecall.screen;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.item.ModItems;
import com.adaptor.deadrecall.item.TieredBackpackItem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class BackpackScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final TieredBackpackItem.BackpackTier tier;

    public static final ScreenHandlerType<BackpackScreenHandler> SCREEN_HANDLER_TYPE =
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of("deadrecall", "backpack"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> {
                    int tierOrdinal = (Integer) data;
                    TieredBackpackItem.BackpackTier tier = TieredBackpackItem.BackpackTier.values()[tierOrdinal];
                    return new BackpackScreenHandler(syncId, playerInventory, tier);
                },
                PacketCodecs.INTEGER
            ));

    // 客户端构造函数（從網路數據讀取等級）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, RegistryByteBuf buf) {
        this(syncId, playerInventory, TieredBackpackItem.BackpackTier.values()[buf.readInt()]);
    }

    // 客户端构造函数（接收等級參數）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, TieredBackpackItem.BackpackTier tier) {
        this(syncId, playerInventory, new SimpleInventory(tier.getSlots()), tier);
    }

    // 客户端构造函数（使用标准等级作为默认）
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(27), TieredBackpackItem.BackpackTier.STANDARD);
    }

    // 服务器端构造函数
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PlayerEntity player, Hand hand, TieredBackpackItem.BackpackTier tier) {
        this(syncId, playerInventory, new BackpackInventory(player, hand, tier), tier);
    }

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, TieredBackpackItem.BackpackTier tier) {
        super(SCREEN_HANDLER_TYPE, syncId);
        this.inventory = inventory;
        this.tier = tier;
        checkSize(inventory, tier.getSlots());
        inventory.onOpen(playerInventory.player);

        // 背包槽位 - 根据等级动态生成
        int rows = tier.getRows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        // 防止背包套娃
                        return !(stack.getItem() instanceof TieredBackpackItem);
                    }
                });
            }
        }

        // 玩家背包位置需要根据背包大小调整
        int playerInventoryY = 18 + rows * 18 + 14; // 背包行数 * 18 + 间距

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

    public TieredBackpackItem.BackpackTier getTier() {
        return tier;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj != null && slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            // 防止背包套娃
            if (originalStack.getItem() instanceof TieredBackpackItem) {
                return ItemStack.EMPTY;
            }

            int backpackSlots = tier.getSlots();

            if (slot < backpackSlots) {
                // 從背包移動到玩家背包
                if (!this.insertItem(originalStack, backpackSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 從玩家背包移動到背包
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
