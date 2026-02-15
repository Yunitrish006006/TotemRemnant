package com.adaptor.deadrecall.entity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class DeathBackpackEntity extends ItemEntity {
    public DeathBackpackEntity(World world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z, stack);
        // lifespan 已在父類中設置為默認值，通過覆蓋tick()來防止消失
    }

    @Override
    public void tick() {
        // 重置age以防止因為時間過期消失
        this.age = 0;
        super.tick();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // 死亡背包實體免疫所有傷害（攻擊、仙人掌、燃燒等）
        return false;
    }

    @Override
    public boolean isFireImmune() {
        // 免疫火焰傷害
        return true;
    }
}
