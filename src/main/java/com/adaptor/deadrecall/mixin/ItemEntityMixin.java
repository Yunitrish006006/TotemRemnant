package com.adaptor.deadrecall.mixin;

import com.adaptor.deadrecall.item.DeathBackpackItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getStack();

    /**
     * 在 ItemEntity.damage() 最開頭注入，
     * 如果這個 ItemEntity 持有的物品是死亡背包，直接回傳 false（免疫所有傷害，包含虛空傷害）。
     * 這個保護是基於物品類型而非實體類別，所以即使區塊重載後實體變回 vanilla ItemEntity 也有效。
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void protectDeathBackpack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.getStack().getItem() instanceof DeathBackpackItem) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 在 ItemEntity.tick() 最開頭注入，
     * 如果死亡背包掉到世界底部以下（虛空），將其向上推回安全位置。
     * - 低於世界底部時：取消重力、給予向上速度，讓背包往上飄
     * - 回到世界底部以上時：恢復正常行為
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void rescueDeathBackpackFromVoid(CallbackInfo ci) {
        if (this.getStack().getItem() instanceof DeathBackpackItem) {
            ItemEntity self = (ItemEntity) (Object) this;
            World world = self.getWorld();
            int bottomY = world.getBottomY();

            // 如果背包掉到世界底部以下，向上推回安全位置
            if (self.getY() < bottomY) {
                // 停止下墜，給予向上速度
                self.setVelocity(self.getVelocity().x * 0.5, 1.5, self.getVelocity().z * 0.5);
                // 如果掉太深，直接傳送到世界底部
                if (self.getY() < bottomY - 32) {
                    self.setPos(self.getX(), bottomY + 1, self.getZ());
                }
                self.setNoGravity(true);
            } else if (self.getY() >= bottomY && self.hasNoGravity()) {
                // 已回到安全高度，停止上升速度，保持懸浮不再下墜
                self.setVelocity(self.getVelocity().x * 0.5, 0.0, self.getVelocity().z * 0.5);
                // 不恢復重力，讓背包永久懸浮在此位置
            }
        }
    }
}

