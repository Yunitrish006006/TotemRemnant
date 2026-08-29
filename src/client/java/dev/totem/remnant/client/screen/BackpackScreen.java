package dev.totem.remnant.client.screen;

import dev.totem.remnant.inventory.BackpackMenu;
import dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import dev.totem.remnant.upgrade.BackpackUpgradeType;

/** Vanilla-styled backpack with adjacent upgrade and embedded crafting panels. */
public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu>
        implements ObserverReadOnlyScreen {
    private static final Identifier CONTAINER_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier CRAFTING_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final int UPGRADE_PANEL_HEIGHT = 40;
    private static final int CRAFTING_PANEL_HEIGHT = 78;
    private static final int BACKGROUND = 0xFFC6C6C6;
    private static final int BORDER_OUTER = 0xFF000000;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int SCROLL_TRACK_X = 171;
    private static final int SCROLL_TRACK_WIDTH = 4;
    public static final int ENDER_BUTTON_X = -22;
    public static final int ENDER_BUTTON_Y = 17;

    private final BackpackMenu backpackMenu;
    private final int visibleRows;
    private int firstVisibleRow;
    private Button enderAccessButton;
    private final boolean observerReadOnly;
    private final Runnable observerStop;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, false, () -> { });
    }

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title,
                          boolean observerReadOnly, Runnable observerStop) {
        super(menu, inventory, title, 176, 114 + Math.min(menu.getRowCount(), MAX_VISIBLE_ROWS) * 18);
        this.backpackMenu = menu;
        this.visibleRows = Math.min(menu.getRowCount(), MAX_VISIBLE_ROWS);
        this.observerReadOnly = observerReadOnly;
        this.observerStop = observerStop;
        this.inventoryLabelY = this.imageHeight - 94;
        applyScrollLayout();
    }

    @Override
    protected void init() {
        super.init();
        // Centre the backpack and both side panels as one combined interface.
        leftPos -= (BackpackMenu.CRAFTING_PANEL_X
                + BackpackMenu.CRAFTING_PANEL_WIDTH - imageWidth) / 2;
        enderAccessButton = Button.builder(Component.literal("◈"), ignored -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(
                                menu.containerId, BackpackMenu.ENDER_ACCESS_BUTTON_ID);
                    }
                })
                .bounds(leftPos + ENDER_BUTTON_X, topPos + ENDER_BUTTON_Y, 20, 18)
                .tooltip(Tooltip.create(Component.translatable(
                        "container.deadrecall.backpack.ender_access")))
                .build();
        enderAccessButton.visible = backpackMenu.hasUpgrade(BackpackUpgradeType.ENDER_ACCESS);
        enderAccessButton.active = !observerReadOnly;
        addRenderableWidget(enderAccessButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (enderAccessButton != null) {
            enderAccessButton.visible = backpackMenu.hasUpgrade(
                    BackpackUpgradeType.ENDER_ACCESS);
        }
    }

    public boolean isEnderAccessButtonVisible() {
        return enderAccessButton != null && enderAccessButton.visible;
    }

    @Override
    public boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if (!super.hasClickedOutside(mouseX, mouseY, left, top)) {
            return false;
        }
        if (insidePanel(mouseX, mouseY, left + BackpackMenu.UPGRADE_PANEL_X, top,
                BackpackMenu.UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT)) {
            return false;
        }
        return !backpackMenu.isCraftingEnabled()
                || !insidePanel(mouseX, mouseY,
                left + BackpackMenu.CRAFTING_PANEL_X,
                top + BackpackMenu.CRAFTING_PANEL_Y,
                BackpackMenu.CRAFTING_PANEL_WIDTH,
                CRAFTING_PANEL_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (observerReadOnly) return true;
        if (verticalAmount != 0.0D && maxScrollRows() > 0
                && isHovering(7, 17, 164, visibleRows * 18 + 2, mouseX, mouseY)) {
            int direction = verticalAmount > 0.0D ? -1 : 1;
            int nextRow = Math.max(0, Math.min(maxScrollRows(), firstVisibleRow + direction));
            if (nextRow != firstVisibleRow) {
                firstVisibleRow = nextRow;
                hoveredSlot = null;
                quickCraftSlots.clear();
                applyScrollLayout();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return observerReadOnly || super.mouseClicked(event, doubled);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return observerReadOnly || super.mouseDragged(event, dragX, dragY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        return observerReadOnly || super.mouseReleased(event);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (!observerReadOnly) return super.keyPressed(event);
        if (event.key() == 256) onClose();
        return true;
    }

    @Override public boolean charTyped(CharacterEvent event) {
        return observerReadOnly || super.charTyped(event);
    }

    @Override public boolean preeditUpdated(PreeditEvent event) {
        return observerReadOnly || super.preeditUpdated(event);
    }

    @Override public void onClose() {
        if (observerReadOnly) observerStop.run();
        else super.onClose();
    }

    @Override public boolean totem$isObserverReadOnly() { return observerReadOnly; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int storageHeight = visibleRows * 18 + 17;
        // A fixed six-row viewport keeps expanded backpacks at the normal GUI scale.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                leftPos, topPos, 0.0F, 0.0F, imageWidth, 17, 256, 256);
        for (int row = 0; row < visibleRows; row++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                    leftPos, topPos + 17 + row * 18, 0.0F, 17.0F,
                    imageWidth, 18, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
                leftPos, topPos + storageHeight, 0.0F, 126.0F,
                imageWidth, 96, 256, 256);
        renderScrollbar(graphics);
        int left = leftPos + BackpackMenu.UPGRADE_PANEL_X;
        int top = topPos;
        renderPanelBackground(graphics, left, top,
                BackpackMenu.UPGRADE_PANEL_WIDTH, UPGRADE_PANEL_HEIGHT);
        int slotStart = left
                + (BackpackMenu.UPGRADE_PANEL_WIDTH - backpackMenu.upgradeSlotCount() * 18) / 2;
        for (int index = 0; index < backpackMenu.upgradeSlotCount(); index++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    slotStart + index * 18, topPos + 17, 18, 18);
        }

        if (backpackMenu.isCraftingEnabled()) {
            int craftingLeft = leftPos + BackpackMenu.CRAFTING_PANEL_X;
            int craftingTop = topPos + BackpackMenu.CRAFTING_PANEL_Y;
            renderPanelBackground(graphics, craftingLeft, craftingTop,
                    BackpackMenu.CRAFTING_PANEL_WIDTH, CRAFTING_PANEL_HEIGHT);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                            leftPos + BackpackMenu.CRAFTING_GRID_X + column * 18,
                            topPos + BackpackMenu.CRAFTING_GRID_Y + row * 18, 18, 18);
                }
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_BACKGROUND,
                    craftingLeft + 62, craftingTop + 34, 89.0F, 34.0F,
                    22, 15, 256, 256);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    leftPos + BackpackMenu.CRAFTING_RESULT_X,
                    topPos + BackpackMenu.CRAFTING_RESULT_Y, 18, 18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component label = Component.translatable("container.deadrecall.backpack.upgrades");
        graphics.text(font, label,
                BackpackMenu.UPGRADE_PANEL_X
                        + (BackpackMenu.UPGRADE_PANEL_WIDTH - font.width(label)) / 2,
                6, 0xFF404040, false);
        if (backpackMenu.isCraftingEnabled()) {
            Component craftingLabel = Component.translatable("container.deadrecall.backpack.crafting");
            graphics.text(font, craftingLabel,
                    BackpackMenu.CRAFTING_PANEL_X
                            + (BackpackMenu.CRAFTING_PANEL_WIDTH - font.width(craftingLabel)) / 2,
                    BackpackMenu.CRAFTING_PANEL_Y + 6, 0xFF404040, false);
        }
    }

    private static void renderPanelBackground(GuiGraphicsExtractor graphics,
                                              int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, BACKGROUND);
        graphics.outline(left, top, width, height, BORDER_OUTER);
        graphics.horizontalLine(left + 1, left + width - 2, top + 1, BORDER_LIGHT);
        graphics.horizontalLine(left + 2, left + width - 3, top + 2, BORDER_LIGHT);
        graphics.verticalLine(left + 1, top + 1, top + height - 2, BORDER_LIGHT);
        graphics.verticalLine(left + 2, top + 2, top + height - 3, BORDER_LIGHT);
        graphics.horizontalLine(left + 2, left + width - 2, top + height - 2, BORDER_DARK);
        graphics.horizontalLine(left + 3, left + width - 3, top + height - 3, BORDER_DARK);
        graphics.verticalLine(left + width - 2, top + 2, top + height - 2, BORDER_DARK);
        graphics.verticalLine(left + width - 3, top + 3, top + height - 3, BORDER_DARK);
    }

    private static boolean insidePanel(double mouseX, double mouseY,
                                       int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width
                && mouseY >= top && mouseY < top + height;
    }

    public int firstVisibleRow() {
        return firstVisibleRow;
    }

    private int maxScrollRows() {
        return Math.max(0, backpackMenu.getRowCount() - visibleRows);
    }

    private void applyScrollLayout() {
        backpackMenu.applyClientScrollLayout(firstVisibleRow, visibleRows);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        int maxScroll = maxScrollRows();
        if (maxScroll == 0) return;
        int trackTop = topPos + 18;
        int trackHeight = visibleRows * 18 - 2;
        int thumbHeight = Math.max(18,
                trackHeight * visibleRows / backpackMenu.getRowCount());
        int travel = trackHeight - thumbHeight;
        int thumbTop = trackTop + travel * firstVisibleRow / maxScroll;
        int trackLeft = leftPos + SCROLL_TRACK_X;
        graphics.fill(trackLeft, trackTop,
                trackLeft + SCROLL_TRACK_WIDTH, trackTop + trackHeight, 0xFF555555);
        graphics.fill(trackLeft + 1, thumbTop,
                trackLeft + SCROLL_TRACK_WIDTH, thumbTop + thumbHeight, 0xFFC6C6C6);
        graphics.verticalLine(trackLeft, thumbTop, thumbTop + thumbHeight - 1, 0xFFFFFFFF);
    }
}
