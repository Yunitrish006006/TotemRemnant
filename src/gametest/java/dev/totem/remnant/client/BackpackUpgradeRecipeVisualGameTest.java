package dev.totem.remnant.client;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Captures one image containing every Remnant upgrade-module recipe. */
@SuppressWarnings("UnstableApiUsage")
public final class BackpackUpgradeRecipeVisualGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        int previousGuiScale = context.computeOnClient(client -> {
            int previous = client.options.guiScale().get();
            client.options.guiScale().set(1);
            client.resizeGui();
            return previous;
        });
        try {
            try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
                singleplayer.getClientLevel().waitForChunksRender();
                context.setScreen(UpgradeRecipeScreen::new);
                context.waitForScreen(UpgradeRecipeScreen.class);
                context.waitTicks(20);
                context.takeScreenshot("backpack-upgrade-recipes");
                context.setScreen(() -> null);
            }
        } finally {
            context.runOnClient(client -> {
                client.options.guiScale().set(previousGuiScale);
                client.resizeGui();
            });
        }
    }

    private static final class UpgradeRecipeScreen extends Screen {
        private static final Identifier SLOT_SPRITE =
                Identifier.withDefaultNamespace("container/slot");
        private static final Identifier CRAFTING_BACKGROUND =
                Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
        private static final int BACKGROUND = 0xFF111722;
        private static final int PANEL = 0xFF202938;
        private static final int CARD = 0xFF303B4D;
        private static final int CARD_BORDER = 0xFF71839A;
        private static final int PRIMARY_TEXT = 0xFFF4F7FB;
        private static final int SECONDARY_TEXT = 0xFFAFC0D5;
        private static final int CARD_WIDTH = 98;
        private static final int CARD_HEIGHT = 92;
        private static final int CARD_GAP = 4;
        private static final int PANEL_WIDTH = CARD_WIDTH * 4 + CARD_GAP * 3 + 8;
        private static final int PANEL_HEIGHT = 328;
        private static final List<RecipeCard> RECIPES = List.of(
                recipe("工作台模組", RemnantItemRegistration.UPGRADE_CRAFTING,
                        Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT,
                        Items.REDSTONE, Items.CRAFTING_TABLE, Items.REDSTONE,
                        Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT),
                recipe("原礦壓縮模組", RemnantItemRegistration.UPGRADE_COMPACTION,
                        Items.IRON_INGOT, Items.PISTON, Items.IRON_INGOT,
                        Items.REDSTONE, Items.CRAFTER, Items.REDSTONE,
                        Items.IRON_INGOT, Items.PISTON, Items.IRON_INGOT),
                recipe("同類收納模組", RemnantItemRegistration.UPGRADE_MATCHING_PICKUP,
                        Items.IRON_INGOT, Items.HOPPER, Items.IRON_INGOT,
                        Items.REDSTONE, Items.CHEST, Items.REDSTONE,
                        Items.IRON_INGOT, Items.HOPPER, Items.IRON_INGOT),
                recipe("容量擴充模組", RemnantItemRegistration.UPGRADE_CAPACITY,
                        Items.LEATHER, Items.BUNDLE, Items.LEATHER,
                        Items.IRON_INGOT, Items.CHEST, Items.IRON_INGOT,
                        Items.LEATHER, Items.BUNDLE, Items.LEATHER),
                recipe("一次性靈魂綁定模組", RemnantItemRegistration.UPGRADE_SOULBOUND_CHARGE,
                        Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
                        Items.ECHO_SHARD, Items.TOTEM_OF_UNDYING, Items.ECHO_SHARD,
                        Items.GOLD_INGOT, Items.NETHERITE_SCRAP, Items.GOLD_INGOT),
                recipe("防爆與仙人掌模組", RemnantItemRegistration.UPGRADE_BLAST_PROTECTION,
                        Items.IRON_INGOT, Items.CACTUS, Items.IRON_INGOT,
                        Items.OBSIDIAN, Items.TNT, Items.OBSIDIAN,
                        Items.IRON_INGOT, Items.CACTUS, Items.IRON_INGOT),
                recipe("防火模組", RemnantItemRegistration.UPGRADE_FIRE_PROTECTION,
                        Items.NETHER_BRICK, Items.BLAZE_POWDER, Items.NETHER_BRICK,
                        Items.MAGMA_CREAM, Items.FIRE_CHARGE, Items.MAGMA_CREAM,
                        Items.NETHER_BRICK, Items.BLAZE_POWDER, Items.NETHER_BRICK),
                recipe("防消失模組", RemnantItemRegistration.UPGRADE_DESPAWN_PROTECTION,
                        Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT,
                        Items.ECHO_SHARD, Items.CLOCK, Items.ECHO_SHARD,
                        Items.GOLD_INGOT, Items.ENDER_PEARL, Items.GOLD_INGOT),
                recipe("虛空防護模組", RemnantItemRegistration.UPGRADE_VOID_PROTECTION,
                        Items.CRYING_OBSIDIAN, Items.ENDER_PEARL, Items.CRYING_OBSIDIAN,
                        Items.CHORUS_FRUIT, Items.RECOVERY_COMPASS, Items.CHORUS_FRUIT,
                        Items.CRYING_OBSIDIAN, Items.ENDER_PEARL, Items.CRYING_OBSIDIAN)
        );

        private UpgradeRecipeScreen() {
            super(Component.literal("TotemRemnant 模組合成表"));
        }

        @Override
        public void extractRenderState(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
        ) {
            graphics.fill(0, 0, width, height, BACKGROUND);
            int panelLeft = (width - PANEL_WIDTH) / 2;
            int panelTop = (height - PANEL_HEIGHT) / 2;
            graphics.fill(panelLeft, panelTop,
                    panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, PANEL);
            graphics.outline(panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFF8293A9);
            graphics.centeredText(font, Component.literal("TotemRemnant 模組合成表（3×3 有序）"),
                    width / 2, panelTop + 7, PRIMARY_TEXT);

            int firstCardLeft = panelLeft + 4;
            int firstCardTop = panelTop + 23;
            for (int index = 0; index < RECIPES.size(); index++) {
                int column = index % 4;
                int row = index / 4;
                renderRecipe(graphics, RECIPES.get(index),
                        firstCardLeft + column * (CARD_WIDTH + CARD_GAP),
                        firstCardTop + row * (CARD_HEIGHT + CARD_GAP));
            }
            graphics.centeredText(font, Component.literal("材料位置不可交換 · 產物皆為 1 個"),
                    width / 2, panelTop + PANEL_HEIGHT - 11, SECONDARY_TEXT);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        private void renderRecipe(GuiGraphicsExtractor graphics, RecipeCard recipe, int left, int top) {
            graphics.fill(left, top, left + CARD_WIDTH, top + CARD_HEIGHT, CARD);
            graphics.outline(left, top, CARD_WIDTH, CARD_HEIGHT, CARD_BORDER);
            graphics.centeredText(font, Component.literal(recipe.title()),
                    left + CARD_WIDTH / 2, top + 5, PRIMARY_TEXT);

            int gridLeft = left + 4;
            int gridTop = top + 20;
            for (int index = 0; index < 9; index++) {
                int x = gridLeft + index % 3 * 18;
                int y = gridTop + index / 3 * 18;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
                graphics.item(recipe.ingredients().get(index), x + 1, y + 1);
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_BACKGROUND,
                    left + 60, top + 39, 89.0F, 34.0F, 15, 15, 256, 256);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    left + 77, top + 37, 18, 18);
            graphics.item(recipe.result(), left + 78, top + 38);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private static RecipeCard recipe(String title, Item result, Item... ingredients) {
            return new RecipeCard(title, new ItemStack(result),
                    java.util.Arrays.stream(ingredients).map(ItemStack::new).toList());
        }
    }

    private record RecipeCard(String title, ItemStack result, List<ItemStack> ingredients) {
    }
}
