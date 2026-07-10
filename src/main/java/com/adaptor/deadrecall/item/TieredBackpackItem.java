package com.adaptor.deadrecall.item;

import com.adaptor.deadrecall.inventory.BackpackInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class TieredBackpackItem extends Item {
    private final BackpackTier tier;

    public TieredBackpackItem(Properties settings, BackpackTier tier) {
        super(settings);
        this.tier = tier;
    }

    public BackpackTier getTier() {
        return tier;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!world.isClientSide() && user instanceof ServerPlayer serverPlayer) {
            BackpackInventory backpackInventory = new BackpackInventory(serverPlayer, hand, tier);
            serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> createChestMenu(syncId, playerInventory, backpackInventory, tier.getRows()),
                Component.translatable("container.deadrecall.backpack." + tier.getName())
            ));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);
        tooltipAdder.accept(Component.translatable(
                "item.deadrecall.backpack.tooltip.tier",
                Component.translatable("item.deadrecall.backpack.tier." + tier.getName())
        ).withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable(
                "item.deadrecall.backpack.tooltip.capacity",
                tier.getSlots(),
                tier.getRows()
        ).withStyle(ChatFormatting.BLUE));
        tooltipAdder.accept(Component.translatable(
                "item.deadrecall.backpack.tooltip.used",
                BackpackItemHelper.countStoredStacks(stack),
                tier.getSlots()
        ).withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable("item.deadrecall.backpack.tooltip.no_nesting").withStyle(ChatFormatting.RED));
        tooltipAdder.accept(Component.translatable("item.deadrecall.backpack.tooltip.death_drop").withStyle(ChatFormatting.GOLD));
        tooltipAdder.accept(Component.translatable(BackpackItemHelper.getProtectionTooltipKey(tier)).withStyle(
                tier == BackpackTier.NETHERITE ? ChatFormatting.GOLD : ChatFormatting.YELLOW
        ));
    }

    private static AbstractContainerMenu createChestMenu(int syncId, Inventory playerInventory, BackpackInventory backpackInventory, int rows) {
        return switch (rows) {
            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInventory, backpackInventory, 1);
            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, syncId, playerInventory, backpackInventory, 2);
            case 3 -> new ChestMenu(MenuType.GENERIC_9x3, syncId, playerInventory, backpackInventory, 3);
            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, syncId, playerInventory, backpackInventory, 4);
            case 5 -> new ChestMenu(MenuType.GENERIC_9x5, syncId, playerInventory, backpackInventory, 5);
            default -> new ChestMenu(MenuType.GENERIC_9x6, syncId, playerInventory, backpackInventory, 6);
        };
    }

    public enum BackpackTier {
        BASIC("basic", "基礎", 9, 1),           // 1排 9格
        STANDARD("standard", "標準", 18, 2),    // 2排 18格
        ADVANCED("advanced", "進階", 27, 3),    // 3排 27格
        NETHERITE("netherite", "獄髓", 36, 4); // 4排 36格

        private final String name;
        private final String displayName;
        private final int slots;
        private final int rows;

        BackpackTier(String name, String displayName, int slots, int rows) {
            this.name = name;
            this.displayName = displayName;
            this.slots = slots;
            this.rows = rows;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSlots() {
            return slots;
        }

        public int getRows() {
            return rows;
        }
    }
}
