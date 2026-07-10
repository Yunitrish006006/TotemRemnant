package com.adaptor.deadrecall.item;

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

public final class BackpackItemHelper {
    private static final double VOID_DAMAGE_MARGIN = 64.0;
    private static final double DROP_BASE_SPEED = 0.35;
    private static final double DROP_RANDOM_SPEED = 0.16;
    private static final double DROP_UPWARD_SPEED = 0.22;
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-6;
    private static final double VOID_RESCUE_UPWARD_SPEED = 0.35;
    private static final double VOID_RESCUE_HORIZONTAL_DAMPING = 0.8;
    private static final double VOID_SLOW_FALL_MAX_DESCENT_SPEED = -0.08;

    private BackpackItemHelper() {
    }

    public static boolean isBackpackItem(ItemStack stack) {
        return stack.getItem() instanceof TieredBackpackItem || stack.getItem() instanceof DeathBackpackItem;
    }

    public static boolean isDeathBackpackItem(ItemStack stack) {
        return stack.getItem() instanceof DeathBackpackItem;
    }

    public static boolean shouldProtectDroppedBackpackFromDamage(ItemStack stack, DamageSource source) {
        if (!isBackpackItem(stack)) {
            return false;
        }

        if (isDeathBackpackItem(stack)) {
            return true;
        }

        if (isVoidDamage(source)) {
            return false;
        }

        return switch (getProtectionLevel(stack)) {
            case BASIC -> false;
            case STANDARD -> isCactusDamage(source);
            case ADVANCED -> isCactusDamage(source) || isExplosionDamage(source);
            case MAXIMUM -> isCactusDamage(source) || isExplosionDamage(source) || isFireDamage(source);
        };
    }

    public static boolean shouldPreventDroppedBackpackDespawn(ItemStack stack) {
        return getProtectionLevel(stack) == BackpackProtectionLevel.MAXIMUM;
    }

    public static boolean shouldApplyDeathBackpackVoidMomentum(ItemEntity itemEntity) {
        return isDeathBackpackItem(itemEntity.getItem())
                && itemEntity.getY() < getVoidDamageY(itemEntity);
    }

    public static boolean shouldStopDeathBackpackVoidMomentum(ItemEntity itemEntity) {
        return isDeathBackpackItem(itemEntity.getItem())
                && itemEntity.isNoGravity()
                && itemEntity.getY() >= itemEntity.level().getMinY();
    }

    public static boolean shouldApplyDeathBackpackSlowFalling(ItemEntity itemEntity) {
        return isDeathBackpackItem(itemEntity.getItem())
                && itemEntity.getY() < itemEntity.level().getMinY();
    }

    public static void applyDeathBackpackVoidMomentum(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(
                movement.x * VOID_RESCUE_HORIZONTAL_DAMPING,
                Math.max(movement.y, VOID_RESCUE_UPWARD_SPEED),
                movement.z * VOID_RESCUE_HORIZONTAL_DAMPING
        );
        itemEntity.setNoGravity(false);
    }

    public static void applyDeathBackpackSlowFalling(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        if (movement.y < VOID_SLOW_FALL_MAX_DESCENT_SPEED) {
            itemEntity.setDeltaMovement(movement.x, VOID_SLOW_FALL_MAX_DESCENT_SPEED, movement.z);
        }
        itemEntity.setNoGravity(false);
    }

    public static void stopDeathBackpackVoidMomentum(ItemEntity itemEntity) {
        Vec3 movement = itemEntity.getDeltaMovement();
        itemEntity.setDeltaMovement(
                movement.x * VOID_RESCUE_HORIZONTAL_DAMPING,
                0.0,
                movement.z * VOID_RESCUE_HORIZONTAL_DAMPING
        );
        itemEntity.setNoGravity(false);
    }

    public static boolean isVoidDamage(DamageSource source) {
        String damageId = normalizeDamageId(source);
        return damageId.contains("outofworld")
                || damageId.contains("felloutofworld")
                || damageId.contains("outsideborder")
                || damageId.contains("void");
    }

    public static String getProtectionTooltipKey(TieredBackpackItem.BackpackTier tier) {
        return switch (tier) {
            case BASIC -> "item.deadrecall.backpack.tooltip.protection.basic";
            case STANDARD -> "item.deadrecall.backpack.tooltip.protection.standard";
            case ADVANCED -> "item.deadrecall.backpack.tooltip.protection.advanced";
            case NETHERITE -> "item.deadrecall.backpack.tooltip.protection.maximum";
        };
    }

    public static String getDeathBackpackProtectionTooltipKey() {
        return "item.deadrecall.death_backpack.tooltip.protection";
    }

    public static int countStoredStacks(ItemStack backpackStack) {
        if (!isBackpackItem(backpackStack)) {
            return 0;
        }

        return (int) backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream()
                .count();
    }

    public static boolean dropStoredItems(ServerLevel level, Vec3 origin, ItemStack backpackStack, Vec3 direction) {
        if (!isBackpackItem(backpackStack)) {
            return false;
        }

        ItemContainerContents contents = backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> drops = contents.nonEmptyItemCopyStream()
                .filter(stack -> !stack.isEmpty())
                .toList();
        if (drops.isEmpty()) {
            return false;
        }

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

    private static double getVoidDamageY(ItemEntity itemEntity) {
        return itemEntity.level().getMinY() - VOID_DAMAGE_MARGIN;
    }

    private static BackpackProtectionLevel getProtectionLevel(ItemStack stack) {
        if (stack.getItem() instanceof DeathBackpackItem) {
            return BackpackProtectionLevel.MAXIMUM;
        }

        if (stack.getItem() instanceof TieredBackpackItem backpackItem) {
            return switch (backpackItem.getTier()) {
                case BASIC -> BackpackProtectionLevel.BASIC;
                case STANDARD -> BackpackProtectionLevel.STANDARD;
                case ADVANCED -> BackpackProtectionLevel.ADVANCED;
                case NETHERITE -> BackpackProtectionLevel.MAXIMUM;
            };
        }

        return BackpackProtectionLevel.BASIC;
    }

    private static boolean isCactusDamage(DamageSource source) {
        return normalizeDamageId(source).contains("cactus");
    }

    private static boolean isExplosionDamage(DamageSource source) {
        String damageId = normalizeDamageId(source);
        return damageId.contains("explosion") || damageId.contains("badrespawnpoint");
    }

    private static boolean isFireDamage(DamageSource source) {
        String damageId = normalizeDamageId(source);
        return damageId.contains("fire")
                || damageId.contains("lava")
                || damageId.contains("hotfloor");
    }

    private static String normalizeDamageId(DamageSource source) {
        return source.getMsgId()
                .replace("_", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
    }

    private static Vec3 createDropVelocity(Vec3 direction, RandomSource random) {
        double speed = DROP_BASE_SPEED + random.nextDouble() * DROP_RANDOM_SPEED;
        double spreadX = (random.nextDouble() - 0.5) * DROP_RANDOM_SPEED;
        double spreadZ = (random.nextDouble() - 0.5) * DROP_RANDOM_SPEED;
        double upward = DROP_UPWARD_SPEED + random.nextDouble() * DROP_RANDOM_SPEED;
        return direction.scale(speed).add(spreadX, upward, spreadZ);
    }

    private static Vec3 normalizeHorizontalOrRandom(Vec3 direction, RandomSource random) {
        Vec3 horizontal = direction == null ? Vec3.ZERO : direction.horizontal();
        if (horizontal.lengthSqr() < MIN_DIRECTION_LENGTH_SQUARED) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }
        return horizontal.normalize();
    }

    private enum BackpackProtectionLevel {
        BASIC,
        STANDARD,
        ADVANCED,
        MAXIMUM
    }
}
