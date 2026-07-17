package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import com.adaptor.deadrecall.inventory.BackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class DeathBackpackItem extends AbstractBackpackItem {
    public DeathBackpackItem(Properties settings) {
        super(settings);
    }

    /**
     * 計算死亡背包實際需要的行數
     */
    public static int calculateRows(ItemStack stack) {
        int storedStacks = BackpackItemHelper.countStoredStacks(stack);
        return Math.max(1, Math.min(6, (int) Math.ceil(storedStacks / 9.0)));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {
            final int rows = calculateRows(stack);
            BackpackInventory backpackInventory = new BackpackInventory(serverPlayer, hand, rows * 9);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> createChestMenu(syncId, playerInventory, backpackInventory, rows),
                Component.translatable("container.deadrecall.death_backpack")
            ));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        int rows = calculateRows(stack);
        int capacity = rows * 9;
        tooltipAdder.accept(Component.translatable("item.deadrecall.death_backpack.tooltip.summary").withStyle(ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable(
            "item.deadrecall.death_backpack.tooltip.capacity",
            BackpackItemHelper.countStoredStacks(stack),
            capacity,
            rows
        ).withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("item.deadrecall.backpack.tooltip.no_nesting").withStyle(ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable("item.deadrecall.death_backpack.tooltip.persistent").withStyle(ChatFormatting.GOLD));
        tooltipAdder.accept(Component.translatable(BackpackItemHelper.getDeathBackpackProtectionTooltipKey()).withStyle(ChatFormatting.GOLD));
    }

    private static AbstractContainerMenu createChestMenu(int syncId, Inventory playerInventory, BackpackInventory backpackInventory, int rows) {
        return switch (rows) {
            case 1 -> new BackpackMenu(MenuType.GENERIC_9x1, syncId, playerInventory, backpackInventory, 1);
            case 2 -> new BackpackMenu(MenuType.GENERIC_9x2, syncId, playerInventory, backpackInventory, 2);
            case 3 -> new BackpackMenu(MenuType.GENERIC_9x3, syncId, playerInventory, backpackInventory, 3);
            case 4 -> new BackpackMenu(MenuType.GENERIC_9x4, syncId, playerInventory, backpackInventory, 4);
            case 5 -> new BackpackMenu(MenuType.GENERIC_9x5, syncId, playerInventory, backpackInventory, 5);
            default -> new BackpackMenu(MenuType.GENERIC_9x6, syncId, playerInventory, backpackInventory, 6);
        };
    }
}
