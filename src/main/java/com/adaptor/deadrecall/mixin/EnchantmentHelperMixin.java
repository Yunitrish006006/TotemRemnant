package com.adaptor.deadrecall.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    /**
     * @author DeadRecall
     * @reason Remove 15 bookshelf cap
     */
    @Overwrite
    public static int getEnchantmentCost(
            RandomSource random,
            int slot,
            int bookcases,
            ItemStack itemStack
    ) {
        var enchantable =
                itemStack.get(net.minecraft.core.component.DataComponents.ENCHANTABLE);

        if (enchantable == null) {
            return 0;
        }

        if (bookcases > 64) {
            bookcases = 64;
        }

        int selected =
                random.nextInt(8)
                        + 1
                        + (bookcases >> 1)
                        + random.nextInt(bookcases + 1);

        if (slot == 0) {
            return Math.max(selected / 3, 1);
        }

        if (slot == 1) {
            return selected * 2 / 3 + 1;
        }

        return Math.min(Math.max(selected, bookcases), 64);
    }
}