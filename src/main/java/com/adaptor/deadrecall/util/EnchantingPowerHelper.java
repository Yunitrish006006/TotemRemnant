package com.adaptor.deadrecall.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

public final class EnchantingPowerHelper {

    private EnchantingPowerHelper() {
    }

    /**
     * 計算附魔台周圍書架提供的有效書櫃值。
     * 直接掃描每本書，而不是先壓成每個書架一個值。
     */
    public static int calculateBookPower(Level level, BlockPos pos) {
        int power = 0;

        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockEntity be = level.getBlockEntity(pos.offset(offset));
            if (be instanceof ChiseledBookShelfBlockEntity shelf) {
                for (ItemStack stack : shelf.getItems()) {
                    power += getItemPower(stack);
                }
            }
        }

        return Math.min(power, 64);
    }

    /**
     * 單本書的權重：
     * - 普通書固定 1
     * - 附魔書依書上附魔等級加總
     */
    public static int getItemPower(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(Items.ENCHANTED_BOOK)) {
            return getBookLevel(stack);
        }

        if (stack.is(Items.BOOK)) {
            return 1;
        }

        return 0;
    }

    /**
     * 讀取附魔書上所有附魔的等級總和。
     */
    private static int getBookLevel(ItemStack stack) {
        var enchantments = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
        );

        int level = 0;
        for (Object2IntMap.Entry<net.minecraft.core.Holder<Enchantment>> entry : enchantments.entrySet()) {
            level += entry.getIntValue();
        }

        return level;
    }
}
