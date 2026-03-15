package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.item.DeathBackpackItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public abstract class CactusBlockMixin {
    /**
     * 攔截仙人掌方塊的 onEntityCollision，
     * 如果碰到的實體是持有死亡背包物品的 ItemEntity，
     * 直接取消整個方法（不對死亡背包造成任何傷害）。
     * 檢查物品本身而非實體類別，這樣即使區塊重載後實體變回 vanilla ItemEntity 也有效。
     */
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void preventDeathBackpackDamage(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getStack().getItem() instanceof DeathBackpackItem) {
                ci.cancel();
            }
        }
    }
}

