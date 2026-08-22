package dev.totem.remnant.death;

import dev.totem.remnant.item.BackpackItemHelper;
import dev.totem.remnant.registry.RemnantItemRegistration;
import dev.totem.remnant.upgrade.BackpackUpgradeData;
import dev.totem.remnant.upgrade.BackpackUpgradeType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/** Regression coverage for the one-slot combined dropped-backpack protection module. */
public final class PerfectPreservationUpgradeGameTest {
    @GameTest(maxTicks = 20)
    public void fourProtectionModulesCraftIntoPerfectPreservation(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 2, List.of(
                new ItemStack(RemnantItemRegistration.UPGRADE_BLAST_PROTECTION),
                new ItemStack(RemnantItemRegistration.UPGRADE_FIRE_PROTECTION),
                new ItemStack(RemnantItemRegistration.UPGRADE_DESPAWN_PROTECTION),
                new ItemStack(RemnantItemRegistration.UPGRADE_VOID_PROTECTION)
        ));
        ItemStack result = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
        require(helper, result.is(RemnantItemRegistration.UPGRADE_PERFECT_PRESERVATION),
                "Four protection modules did not craft the Perfect Preservation module");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void perfectPreservationProvidesAllDroppedBackpackProtections(GameTestHelper helper) {
        ItemStack backpack = new ItemStack(RemnantItemRegistration.BACKPACK_BASIC);
        BackpackUpgradeData.write(backpack,
                List.of(new ItemStack(RemnantItemRegistration.UPGRADE_PERFECT_PRESERVATION)), 1);

        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.BLAST_PROTECTION),
                "Perfect Preservation did not provide blast protection");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.FIRE_PROTECTION),
                "Perfect Preservation did not provide fire protection");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.DESPAWN_PROTECTION),
                "Perfect Preservation did not provide despawn protection");
        require(helper, BackpackUpgradeData.has(backpack, BackpackUpgradeType.VOID_PROTECTION),
                "Perfect Preservation did not provide void protection");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        backpack, helper.getLevel().damageSources().explosion(null, null)),
                "Perfect Preservation did not reject explosion damage");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        backpack, helper.getLevel().damageSources().lava()),
                "Perfect Preservation did not reject fire/lava damage");
        require(helper, BackpackItemHelper.shouldPreventDroppedBackpackDespawn(backpack),
                "Perfect Preservation did not prevent natural despawn");
        require(helper, BackpackItemHelper.shouldProtectDroppedBackpackFromDamage(
                        backpack, helper.getLevel().damageSources().fellOutOfWorld()),
                "Perfect Preservation did not reject void damage");

        ItemEntity entity = new ItemEntity(helper.getLevel(), 0.5,
                helper.getLevel().getMinY() - 65.0, 0.5, backpack);
        require(helper, BackpackItemHelper.shouldApplyBackpackVoidMomentum(entity),
                "Perfect Preservation did not activate void rescue");
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
