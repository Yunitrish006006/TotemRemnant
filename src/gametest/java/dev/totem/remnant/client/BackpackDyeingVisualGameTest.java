package dev.totem.remnant.client;

import dev.totem.remnant.registry.RemnantItemRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

/** Captures the real item renderer output for four representative dyed backpack tiers. */
@SuppressWarnings("UnstableApiUsage")
public final class BackpackDyeingVisualGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            context.setScreen(BackpackDyeingShowcaseScreen::new);
            context.waitForScreen(BackpackDyeingShowcaseScreen.class);
            context.waitTicks(20);
            context.takeScreenshot("backpack-dyeing-four-tiers");
            context.setScreen(() -> null);
        }
    }

    private static final class BackpackDyeingShowcaseScreen extends Screen {
        private static final int BACKGROUND = 0xFF11141C;
        private static final int PANEL = 0xEE202633;
        private static final int CARD = 0xFF2C3443;
        private static final int CARD_BORDER = 0xFF59677C;
        private static final int PRIMARY_TEXT = 0xFFF5F7FA;
        private static final int SECONDARY_TEXT = 0xFFAAB7C8;
        private static final int CARD_WIDTH = 84;
        private static final int CARD_HEIGHT = 134;
        private static final int CARD_GAP = 8;
        private static final int PANEL_WIDTH = CARD_WIDTH * 4 + CARD_GAP * 3 + 24;
        private static final int PANEL_HEIGHT = 220;
        private static final List<ShowcaseBackpack> BACKPACKS = List.of(
                backpack(RemnantItemRegistration.BACKPACK_BASIC, DyeColor.RED, "基礎背包", "紅色"),
                backpack(RemnantItemRegistration.BACKPACK_STANDARD, DyeColor.YELLOW, "標準背包", "黃色"),
                backpack(RemnantItemRegistration.BACKPACK_ADVANCED, DyeColor.CYAN, "進階背包", "青色"),
                backpack(RemnantItemRegistration.BACKPACK_NETHERITE, DyeColor.PURPLE, "獄髓背包", "紫色")
        );

        private BackpackDyeingShowcaseScreen() {
            super(Component.literal("TotemRemnant 染色背包"));
        }

        @Override
        public void extractRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            graphics.fill(0, 0, width, height, BACKGROUND);
            int panelLeft = (width - PANEL_WIDTH) / 2;
            int panelTop = (height - PANEL_HEIGHT) / 2;
            graphics.fill(
                    panelLeft,
                    panelTop,
                    panelLeft + PANEL_WIDTH,
                    panelTop + PANEL_HEIGHT,
                    PANEL
            );
            graphics.outline(panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFF75849A);
            graphics.centeredText(
                    font,
                    Component.literal("TotemRemnant 染色背包"),
                    width / 2,
                    panelTop + 14,
                    PRIMARY_TEXT
            );
            graphics.centeredText(
                    font,
                    Component.literal("四級背包皆使用遊戲內 minecraft:dyed_color 即時渲染"),
                    width / 2,
                    panelTop + 29,
                    SECONDARY_TEXT
            );

            int firstCardLeft = panelLeft + 12;
            int cardTop = panelTop + 48;
            for (int index = 0; index < BACKPACKS.size(); index++) {
                int cardLeft = firstCardLeft + index * (CARD_WIDTH + CARD_GAP);
                renderCard(graphics, BACKPACKS.get(index), cardLeft, cardTop);
            }

            graphics.centeredText(
                    font,
                    Component.literal("工作台染色與混色  ·  裝水煉藥鍋洗色"),
                    width / 2,
                    panelTop + PANEL_HEIGHT - 17,
                    SECONDARY_TEXT
            );
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        private void renderCard(
                GuiGraphicsExtractor graphics,
                ShowcaseBackpack showcase,
                int left,
                int top
        ) {
            graphics.fill(left, top, left + CARD_WIDTH, top + CARD_HEIGHT, CARD);
            graphics.outline(left, top, CARD_WIDTH, CARD_HEIGHT, CARD_BORDER);

            graphics.pose().pushMatrix();
            graphics.pose().translate(left + 18, top + 13);
            graphics.pose().scale(3.0F, 3.0F);
            graphics.item(showcase.stack(), 0, 0);
            graphics.pose().popMatrix();

            graphics.centeredText(
                    font,
                    Component.literal(showcase.tierLabel()),
                    left + CARD_WIDTH / 2,
                    top + 72,
                    PRIMARY_TEXT
            );
            graphics.centeredText(
                    font,
                    Component.literal(showcase.colorLabel()),
                    left + CARD_WIDTH / 2,
                    top + 89,
                    showcase.textColor()
            );
            graphics.fill(
                    left + 24,
                    top + 111,
                    left + CARD_WIDTH - 24,
                    top + 117,
                    0xFF000000 | showcase.rgb()
            );
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private static ShowcaseBackpack backpack(
                Item item,
                DyeColor color,
                String tierLabel,
                String colorLabel
        ) {
            DyedItemColor dyedColor = DyedItemColor.applyDyes(
                    (DyedItemColor) null,
                    List.of(color)
            );
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.DYED_COLOR, dyedColor);
            return new ShowcaseBackpack(
                    stack,
                    tierLabel,
                    colorLabel,
                    dyedColor.rgb(),
                    0xFF000000 | color.getTextColor()
            );
        }
    }

    private record ShowcaseBackpack(
            ItemStack stack,
            String tierLabel,
            String colorLabel,
            int rgb,
            int textColor
    ) {
    }
}
