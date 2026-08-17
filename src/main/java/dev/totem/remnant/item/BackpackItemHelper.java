package dev.totem.remnant.item;

import dev.totem.remnant.upgrade.BackpackUpgradeData;
import dev.totem.remnant.upgrade.BackpackUpgradeType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/** Remnant backpack helpers used by capture and nesting policy. */
public final class BackpackItemHelper {
    private static final double VOID_DAMAGE_MARGIN = 64.0;
    private static final double DROP_BASE_SPEED = 0.35;
    private static final double DROP_RANDOM_SPEED = 0.16;
    private static final double DROP_UPWARD_SPEED = 0.22;
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-6;
    private static final double VOID_RESCUE_UPWARD_SPEED = 0.35;
    private static final double VOID_RESCUE_HORIZONTAL_DAMPING = 0.8;
    private static final double VOID_SLOW_FALL_MAX_DESCENT_SPEED = -0.08;

    private BackpackItemHelper() { }

    public static boolean isBackpackItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof AbstractBackpackItem;
    }

    public static int countStoredStacks(ItemStack backpackStack) {
        if (!isBackpackItem(backpackStack)) return 0;
        return (int) backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().count();
    }

    /** Number of serialized slots through the final non-empty item, including intentional gaps. */
    public static int storedSlotFootprint(ItemStack backpackStack) {
        if (!isBackpackItem(backpackStack)) return 0;
        return (int) backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .allItemsCopyStream().count();
    }

    public static boolean isDeathBackpackItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof DeathBackpackItem;
    }

    public static boolean shouldProtectDroppedBackpackFromDamage(ItemStack stack, DamageSource source) {
        if (!isBackpackItem(stack)) return false;
        if (isDeathBackpackItem(stack)) return true;
        if (isVoidDamage(source)) {
            return BackpackUpgradeData.has(stack, BackpackUpgradeType.VOID_PROTECTION);
        }
        return (isCactusDamage(source) || isExplosionDamage(source))
                        && BackpackUpgradeData.has(stack, BackpackUpgradeType.BLAST_PROTECTION)
                || isFireDamage(source) && (isNetheriteBackpack(stack)
                        || BackpackUpgradeData.has(stack, BackpackUpgradeType.FIRE_PROTECTION));
    }

    public static boolean shouldPreventDroppedBackpackDespawn(ItemStack stack) {
        return isDeathBackpackItem(stack)
                || BackpackUpgradeData.has(stack, BackpackUpgradeType.DESPAWN_PROTECTION);
    }

    public static boolean shouldApplyBackpackVoidMomentum(ItemEntity itemEntity) {
        return hasVoidProtection(itemEntity.getItem()) && itemEntity.getY() < getVoidDamageY(itemEntity);
    }
    public static boolean shouldStopBackpackVoidMomentum(ItemEntity itemEntity) {
        return hasVoidProtection(itemEntity.getItem())
                && itemEntity.isNoGravity() && itemEntity.getY() >= itemEntity.level().getMinY();
    }
    public static boolean shouldApplyBackpackSlowFalling(ItemEntity itemEntity) {
        return hasVoidProtection(itemEntity.getItem()) && itemEntity.getY() < itemEntity.level().getMinY();
    }
    public static void applyBackpackVoidMomentum(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(movement.x * VOID_RESCUE_HORIZONTAL_DAMPING,
                Math.max(movement.y, VOID_RESCUE_UPWARD_SPEED), movement.z * VOID_RESCUE_HORIZONTAL_DAMPING);
        itemEntity.setNoGravity(false);
    }
    public static void applyBackpackSlowFalling(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        if (movement.y < VOID_SLOW_FALL_MAX_DESCENT_SPEED) itemEntity.setDeltaMovement(movement.x, VOID_SLOW_FALL_MAX_DESCENT_SPEED, movement.z);
        itemEntity.setNoGravity(false);
    }
    public static void stopBackpackVoidMomentum(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(movement.x * VOID_RESCUE_HORIZONTAL_DAMPING, 0.0, movement.z * VOID_RESCUE_HORIZONTAL_DAMPING);
        itemEntity.setNoGravity(false);
    }

    public static boolean isVoidDamage(DamageSource source) {
        String damageId = normalizeDamageId(source);
        return damageId.contains("outofworld") || damageId.contains("felloutofworld")
                || damageId.contains("outsideborder") || damageId.contains("void");
    }

    public static boolean dropStoredItems(ServerLevel level, Vec3 origin, ItemStack backpackStack, Vec3 direction) {
        if (!isBackpackItem(backpackStack)) return false;
        List<ItemStack> drops = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().filter(stack -> !stack.isEmpty()).toList();
        if (drops.isEmpty()) return false;
        RandomSource random = level.getRandom();
        Vec3 ejectionDirection = normalizeHorizontalOrRandom(direction, random);
        for (ItemStack drop : drops) {
            ItemEntity itemEntity = new ItemEntity(level, origin.x, origin.y + 0.1, origin.z, drop.copy());
            itemEntity.setDeltaMovement(createDropVelocity(ejectionDirection, random));
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
        return true;
    }

    private static double getVoidDamageY(ItemEntity itemEntity) { return itemEntity.level().getMinY() - VOID_DAMAGE_MARGIN; }
    private static boolean hasVoidProtection(ItemStack stack) {
        return isDeathBackpackItem(stack)
                || BackpackUpgradeData.has(stack, BackpackUpgradeType.VOID_PROTECTION);
    }
    private static boolean isNetheriteBackpack(ItemStack stack) {
        return stack.getItem() instanceof TieredBackpackItem backpack
                && backpack.tier() == TieredBackpackItem.BackpackTier.NETHERITE;
    }
    private static boolean isCactusDamage(DamageSource source) { return normalizeDamageId(source).contains("cactus"); }
    private static boolean isExplosionDamage(DamageSource source) {
        String id = normalizeDamageId(source); return id.contains("explosion") || id.contains("badrespawnpoint");
    }
    private static boolean isFireDamage(DamageSource source) {
        String id = normalizeDamageId(source); return id.contains("fire") || id.contains("lava") || id.contains("hotfloor");
    }
    private static String normalizeDamageId(DamageSource source) {
        return source.getMsgId().replace("_", "").replace(".", "").toLowerCase(Locale.ROOT);
    }
    private static Vec3 createDropVelocity(Vec3 direction, RandomSource random) {
        double speed = DROP_BASE_SPEED + random.nextDouble() * DROP_RANDOM_SPEED;
        return direction.scale(speed).add((random.nextDouble() - .5) * DROP_RANDOM_SPEED,
                DROP_UPWARD_SPEED + random.nextDouble() * DROP_RANDOM_SPEED, (random.nextDouble() - .5) * DROP_RANDOM_SPEED);
    }
    private static Vec3 normalizeHorizontalOrRandom(Vec3 direction, RandomSource random) {
        Vec3 horizontal = direction == null ? Vec3.ZERO : direction.horizontal();
        if (horizontal.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        return horizontal.normalize();
    }
}
