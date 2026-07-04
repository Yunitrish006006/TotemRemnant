package com.adaptor.deadrecall.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import com.adaptor.deadrecall.util.EnchantingPowerHelper;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Shadow @Final
    private ContainerLevelAccess access;

    @Shadow @Final
    private net.minecraft.util.RandomSource random;

    @Shadow @Final
    private net.minecraft.world.inventory.DataSlot enchantmentSeed;

    @Shadow @Final
    public int[] costs;

    @Shadow @Final
    public int[] enchantClue;

    @Shadow @Final
    public int[] levelClue;

    @Shadow
    private List<EnchantmentInstance> getEnchantmentList(net.minecraft.core.RegistryAccess access, ItemStack itemStack, int slot, int enchantmentCost) {
        return null;
    }

    @Shadow @Final
    private Container enchantSlots;

    /**
     * 完全替換 slotsChanged，使用自定義書架力量計算
     * @author DeadRecall
     * @reason 自定義書架系統，根據書本數量和等級計算附魔力
     */
    @Overwrite
    public void slotsChanged(Container container) {
        if (container == this.enchantSlots) {
            ItemStack itemStack = container.getItem(0);
            if (!itemStack.isEmpty() && itemStack.isEnchantable()) {
                this.access.execute((level, pos) -> {
                    int bookcases = EnchantingPowerHelper.calculateBookPower(level, pos);

                    this.random.setSeed((long) this.enchantmentSeed.get());

                    for (int i = 0; i < 3; ++i) {
                        this.costs[i] = EnchantmentHelper.getEnchantmentCost(this.random, i, bookcases, itemStack);
                        this.enchantClue[i] = -1;
                        this.levelClue[i] = -1;
                        if (this.costs[i] < i + 1) {
                            this.costs[i] = 0;
                        }
                    }

                    for (int i = 0; i < 3; ++i) {
                        if (this.costs[i] > 0) {
                            List<EnchantmentInstance> list = this.getEnchantmentList(level.registryAccess(), itemStack, i, this.costs[i]);
                            if (!list.isEmpty()) {
                                net.minecraft.core.IdMap<net.minecraft.core.Holder<Enchantment>> holders = 
                                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                                EnchantmentInstance ench = list.get(this.random.nextInt(list.size()));
                                this.enchantClue[i] = holders.getId(ench.enchantment());
                                this.levelClue[i] = ench.level();
                            }
                        }
                    }

                    ((net.minecraft.world.inventory.AbstractContainerMenu) (Object) this).broadcastChanges();
                });
            } else {
                for (int i = 0; i < 3; ++i) {
                    this.costs[i] = 0;
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                }
            }
        }
    }

}