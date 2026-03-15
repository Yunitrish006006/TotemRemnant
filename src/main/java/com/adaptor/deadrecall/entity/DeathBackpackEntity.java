package com.adaptor.deadrecall.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 死亡背包實體 — 不會被任何方式破壞（仙人掌、火焰、爆炸等）。
 * 多層保護：
 *  1. CactusBlockMixin 從源頭攔截仙人掌碰撞（不讓仙人掌呼叫 damage）
 *  2. damage() 覆寫 → 直接回傳 false（免疫所有傷害）
 *  3. isInvulnerableTo() 覆寫 → 回傳 true（標記為對所有傷害來源無敵）
 *  4. isFireImmune() 覆寫 → 回傳 true
 *  5. tick() 中偵測仙人掌方塊並把實體推離
 */
public class DeathBackpackEntity extends ItemEntity {

    private boolean isDeathBackpack = true;

    public DeathBackpackEntity(World world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z, stack);
        this.setInvulnerable(true);
    }

    @Override
    public void tick() {
        // 重置 age 以防止因為時間過期消失
        this.age = 0;

        // 如果背包實體卡在仙人掌方塊中，將其向上推離
        try {
            BlockPos pos = this.getBlockPos();
            World world = this.getWorld();
            if (!world.isClient) {
                if (world.getBlockState(pos).isOf(Blocks.CACTUS)) {
                    this.setPos(this.getX(), this.getY() + 0.6, this.getZ());
                    this.setVelocity(this.getVelocity().x, 0.1, this.getVelocity().z);
                }
            }
        } catch (Exception ignored) {
        }

        super.tick();
    }

    // ===== 多層傷害免疫 =====

    @Override
    public boolean damage(DamageSource source, float amount) {
        // 死亡背包實體免疫所有傷害（仙人掌、攻擊、燃燒、爆炸等）
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        // 對所有傷害來源標記為無敵
        return true;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }
}
