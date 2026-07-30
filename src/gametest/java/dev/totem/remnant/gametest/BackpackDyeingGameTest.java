package dev.totem.remnant.gametest;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.List;

public final class BackpackDyeingGameTest {
    private static final BlockPos CAULDRON_POS = new BlockPos(2, 1, 2);
    private static final Component CUSTOM_NAME = Component.literal("採礦用品");
    private static final CustomData ADDON_DATA = addonData();

    @GameTest(maxTicks = 20)
    public void everyTierUsesVanillaDyeMixingAndPreservesComponents(GameTestHelper helper) {
        List<Item> tiers = List.of(
                RemnantItemRegistration.BACKPACK_BASIC,
                RemnantItemRegistration.BACKPACK_STANDARD,
                RemnantItemRegistration.BACKPACK_ADVANCED,
                RemnantItemRegistration.BACKPACK_NETHERITE
        );
        DyedItemColor expectedColor = DyedItemColor.applyDyes(
                (DyedItemColor) null,
                List.of(DyeColor.RED, DyeColor.BLUE)
        );

        for (Item tier : tiers) {
            ItemStack backpack = populatedBackpack(tier);
            CraftingInput input = CraftingInput.of(3, 1, List.of(
                    backpack,
                    new ItemStack(Items.DYE.pick(DyeColor.RED)),
                    new ItemStack(Items.DYE.pick(DyeColor.BLUE))
            ));
            RecipeHolder<CraftingRecipe> holder = helper.getLevel().recipeAccess()
                    .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                    .orElseThrow(() -> helper.assertionException("Missing dye recipe for " + tier));
            ItemStack dyed = holder.value().assemble(input);

            require(helper, dyed.is(tier), "Dye recipe changed backpack tier for " + tier);
            require(helper, expectedColor.equals(dyed.get(DataComponents.DYED_COLOR)),
                    "Dye recipe did not use vanilla red/blue mixing for " + tier);
            requirePreservedComponents(helper, dyed, "Dye recipe");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void existingColorParticipatesInLaterMixes(GameTestHelper helper) {
        ItemStack backpack = populatedBackpack(RemnantItemRegistration.BACKPACK_ADVANCED);
        DyedItemColor initialColor = new DyedItemColor(0x287F38);
        backpack.set(DataComponents.DYED_COLOR, initialColor);
        CraftingInput input = CraftingInput.of(2, 1, List.of(
                backpack,
                new ItemStack(Items.DYE.pick(DyeColor.WHITE))
        ));
        RecipeHolder<CraftingRecipe> holder = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("Missing repeated dye recipe"));
        ItemStack dyed = holder.value().assemble(input);
        DyedItemColor expected = DyedItemColor.applyDyes(initialColor, List.of(DyeColor.WHITE));

        require(helper, expected.equals(dyed.get(DataComponents.DYED_COLOR)),
                "Existing color was not included in vanilla dye mixing");
        requirePreservedComponents(helper, dyed, "Repeated dye recipe");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void waterCauldronRemovesOnlyBackpackColor(GameTestHelper helper) {
        ItemStack backpack = populatedBackpack(RemnantItemRegistration.BACKPACK_STANDARD);
        backpack.set(DataComponents.DYED_COLOR, new DyedItemColor(0xC43C35));
        require(helper, backpack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE),
                "Tiered backpack is missing the vanilla cauldron dye-removal tag");

        helper.setBlock(
                CAULDRON_POS,
                Blocks.WATER_CAULDRON.defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL)
        );
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, backpack);
        helper.useBlock(CAULDRON_POS, player);

        ItemStack washed = player.getMainHandItem();
        require(helper, !washed.has(DataComponents.DYED_COLOR),
                "Water cauldron did not remove backpack dyed color");
        requirePreservedComponents(helper, washed, "Water cauldron");
        require(helper, helper.getBlockState(CAULDRON_POS).getValue(LayeredCauldronBlock.LEVEL)
                        == LayeredCauldronBlock.MAX_FILL_LEVEL - 1,
                "Water cauldron did not consume exactly one level");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void deathBackpackIsExcludedFromDyeAndWashFlows(GameTestHelper helper) {
        ItemStack deathBackpack = new ItemStack(RemnantItemRegistration.DEATH_BACKPACK);
        CraftingInput input = CraftingInput.of(2, 1, List.of(
                deathBackpack,
                new ItemStack(Items.DYE.pick(DyeColor.RED))
        ));

        require(helper, helper.getLevel().recipeAccess()
                        .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                        .isEmpty(),
                "Death backpack unexpectedly matched a dye recipe");
        require(helper, !deathBackpack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE),
                "Death backpack unexpectedly joined the cauldron dye-removal tag");
        helper.succeed();
    }

    private static ItemStack populatedBackpack(Item item) {
        ItemStack backpack = new ItemStack(item);
        backpack.set(DataComponents.CONTAINER, contents());
        backpack.set(DataComponents.CUSTOM_NAME, CUSTOM_NAME);
        backpack.set(DataComponents.CUSTOM_DATA, ADDON_DATA);
        return backpack;
    }

    private static void requirePreservedComponents(
            GameTestHelper helper,
            ItemStack backpack,
            String operation
    ) {
        require(helper, contents().equals(backpack.get(DataComponents.CONTAINER)),
                operation + " changed stored contents");
        require(helper, CUSTOM_NAME.equals(backpack.get(DataComponents.CUSTOM_NAME)),
                operation + " changed the custom name");
        require(helper, ADDON_DATA.equals(backpack.get(DataComponents.CUSTOM_DATA)),
                operation + " changed addon custom data");
    }

    private static CustomData addonData() {
        CompoundTag tag = new CompoundTag();
        tag.putString("test_owner", "totem-remnant");
        tag.putInt("test_revision", 1);
        return CustomData.of(tag);
    }

    private static ItemContainerContents contents() {
        return ItemContainerContents.fromItems(List.of(
                new ItemStack(Items.DIAMOND, 3),
                new ItemStack(Items.TORCH, 16)
        ));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
