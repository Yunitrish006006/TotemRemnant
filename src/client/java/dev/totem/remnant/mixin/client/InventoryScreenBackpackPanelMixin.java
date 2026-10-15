package dev.totem.remnant.mixin.client;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.inventory.BackpackPanelContainer;
import dev.totem.remnant.network.SelectBackpackPanelPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;


import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import java.util.Set;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Places the InventoryMenu's real selected-backpack slots beside vanilla inventory. */
@Mixin(AbstractContainerScreen.class)
abstract class InventoryScreenBackpackPanelMixin extends Screen implements dev.totem.remnant.client.screen.BackpackSidebarLayout {
    @Unique private static final int totem$PREFERRED_COLUMNS = 9;
    @Unique private static final int totem$MIN_COLUMNS = 1;
    @Unique private static final int totem$SLOT_SIZE = 18;
    @Unique private static final int totem$PANEL_HORIZONTAL_PADDING = 14;
    @Unique private static final int totem$HEADER_HEIGHT = 17;
    @Unique private static final int totem$PANEL_PADDING_BOTTOM = 7;
    @Unique private static final int totem$PANEL_GAP = 6;
    @Unique private static final int totem$BACKGROUND = 0xFFC6C6C6;
    @Unique private static final int totem$BORDER_OUTER = 0xFF000000;
    @Unique private static final int totem$BORDER_LIGHT = 0xFFFFFFFF;
    @Unique private static final int totem$BORDER_DARK = 0xFF555555;
    @Unique private static final Identifier totem$SLOT_SPRITE =
            Identifier.withDefaultNamespace("container/slot");

    @Unique private int totem$selectedInventorySlot = -1;

    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected Set<Slot> quickCraftSlots;
    @Unique private int totem$firstRow;
    @Unique private int totem$columns = 9;
    @Unique private int totem$visibleRows;
    @Unique private int totem$panelLeft, totem$panelTop, totem$panelWidth, totem$panelHeight;

    protected InventoryScreenBackpackPanelMixin(Component title) { super(title); }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void totem$insidePanel(double mouseX, double mouseY, int left, int top,
                                  CallbackInfoReturnable<Boolean> callback) {
        if (totem$overPanel(mouseX, mouseY)) callback.setReturnValue(false);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void totem$scrollPanel(double mouseX, double mouseY, double dx, double dy,
                                   CallbackInfoReturnable<Boolean> callback) {
        if (totem$observerReadOnly()) { callback.setReturnValue(true); return; }
        if (!totem$overPanel(mouseX, mouseY) || !(BackpackPanelMenuAccess.supports(menu) && menu instanceof BackpackPanelMenuAccess access)) return;
        int rows = (access.totem$getBackpackPanel().activeSlotCount() + totem$columns - 1) / totem$columns;
        totem$firstRow = Math.clamp(totem$firstRow - (int)Math.signum(dy), 0, Math.max(0, rows - totem$visibleRows));
        totem$clearTransientSlotState();
        callback.setReturnValue(true);
    }

    @Unique private boolean totem$overPanel(double x, double y) {
        return BackpackPanelMenuAccess.supports(menu) && totem$panelHeight > 0
                && x >= totem$panelLeft && x < totem$panelLeft + totem$panelWidth
                && y >= totem$panelTop && y < totem$panelTop + totem$panelHeight;
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void totem$prepareInteractiveBackpackPanel(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if (totem$observerReadOnly() && !((Object)this instanceof dev.totem.remnant.client.screen.SidebarProjection)) return;
        if (minecraft == null || minecraft.player == null
                || !(BackpackPanelMenuAccess.supports(menu) && menu instanceof BackpackPanelMenuAccess access)) {
            return;
        }
        Inventory inventory = access.totem$getBackpackPanel().playerInventory();
        List<Integer> backpacks = totem$ordinaryBackpackSlots(inventory);
        if (backpacks.isEmpty()) {
            totem$panelHeight = 0;
            totem$selectBackpack(access, -1);
            if (access.totem$layoutBackpackSlots(-10_000, -10_000, 1)) {
                totem$clearTransientSlotState();
            }
            return;
        }

        if (!totem$observerReadOnly() && hoveredSlot != null && hoveredSlot.container == inventory
                && BackpackPanelContainer.isBackpack(hoveredSlot.getItem())) {
            totem$selectBackpack(access, hoveredSlot.getContainerSlot());
        }
        if (totem$observerReadOnly()) totem$selectedInventorySlot = access.totem$getBackpackPanel().selectedInventorySlot();
        if (!backpacks.contains(totem$selectedInventorySlot)) {
            int menuSelection = access.totem$getBackpackPanel().selectedInventorySlot();
            totem$selectBackpack(
                    access,
                    backpacks.contains(menuSelection) ? menuSelection : backpacks.getFirst()
            );
        }

        ItemStack backpack = inventory.getItem(totem$selectedInventorySlot);
        if (!BackpackPanelContainer.isBackpack(backpack)) {
            return;
        }
        int columns = totem$panelColumns(graphics.guiWidth());
        if (columns == 0) {
            totem$panelHeight=0;
            if(access.totem$layoutBackpackWindow(-10_000,-10_000,1,0,0))totem$clearTransientSlotState();
            return;
        }
        int panelWidth = totem$PANEL_HORIZONTAL_PADDING + columns * totem$SLOT_SIZE;
        int storageSlots = access.totem$getBackpackPanel().activeSlotCount();
        int allRows = (storageSlots + columns - 1) / columns;
        int rows = Math.min(allRows, Math.max(1, (graphics.guiHeight() - 8 - totem$HEADER_HEIGHT - totem$PANEL_PADDING_BOTTOM) / totem$SLOT_SIZE));
        totem$firstRow = Math.clamp(totem$firstRow, 0, Math.max(0, allRows - rows));
        totem$columns = columns;
        totem$visibleRows = rows;
        int panelHeight = totem$HEADER_HEIGHT + rows * totem$SLOT_SIZE
                + totem$PANEL_PADDING_BOTTOM;
        int panelLeft = totem$panelLeft(graphics.guiWidth(), panelWidth);
        int panelTop = Math.max(4, Math.min(
                topPos + (imageHeight - panelHeight) / 2,
                graphics.guiHeight() - panelHeight - 4
        ));

        totem$panelLeft = panelLeft; totem$panelTop = panelTop;
        totem$panelWidth = panelWidth; totem$panelHeight = panelHeight;
        if (access.totem$layoutBackpackWindow(
                panelLeft + 7 - leftPos,
                panelTop + totem$HEADER_HEIGHT - topPos,
                columns, totem$firstRow, rows
        )) {
            totem$clearTransientSlotState();
        }

        totem$renderPanelBackground(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        for (int slot = 0; slot < Math.min(rows * columns, storageSlots - totem$firstRow * columns); slot++) {
            int slotLeft = panelLeft + 7 + slot % columns * totem$SLOT_SIZE;
            int slotTop = panelTop + totem$HEADER_HEIGHT
                    + slot / columns * totem$SLOT_SIZE;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    totem$SLOT_SPRITE,
                    slotLeft,
                    slotTop,
                    totem$SLOT_SIZE,
                    totem$SLOT_SIZE
            );
        }

        if (allRows > rows) {
            int trackX=panelLeft+panelWidth-5, trackY=panelTop+totem$HEADER_HEIGHT;
            int trackHeight=rows*totem$SLOT_SIZE;
            int thumbHeight=Math.max(4,trackHeight*rows/allRows);
            int thumbY=trackY+(trackHeight-thumbHeight)*totem$firstRow/Math.max(1,allRows-rows);
            graphics.fill(trackX,trackY,trackX+2,trackY+trackHeight,totem$BORDER_DARK);
            graphics.fill(trackX,thumbY,trackX+2,thumbY+thumbHeight,totem$BORDER_LIGHT);
        }

        String counter = (backpacks.indexOf(totem$selectedInventorySlot) + 1)
                + "/" + backpacks.size();
        int counterLeft = panelLeft + panelWidth - 8 - font.width(counter);
        String title = totem$fitTitle(
                backpack.getHoverName().getString(),
                Math.max(0, counterLeft - (panelLeft + 8) - 3)
        );
        graphics.text(font, title, panelLeft + 8, panelTop + 6, 0xFF404040, false);
        graphics.text(font, counter, counterLeft, panelTop + 6, 0xFF404040, false);
    }

    @Unique
    private void totem$selectBackpack(BackpackPanelMenuAccess access, int inventorySlot) {
        if (totem$observerReadOnly()) return;
        if (inventorySlot == totem$selectedInventorySlot) {
            return;
        }
        totem$selectedInventorySlot = inventorySlot;
        totem$firstRow = 0;
        access.totem$selectBackpackSlot(inventorySlot);
        if (ClientPlayNetworking.canSend(SelectBackpackPanelPayload.TYPE)) {
            ClientPlayNetworking.send(new SelectBackpackPanelPayload(menu.containerId, inventorySlot));
        }
        totem$clearTransientSlotState();
    }

    @Override public boolean remnant$contains(double x,double y) { return totem$overPanel(x,y); }
    @Override public int remnant$left() { return leftPos; }
    @Override public int remnant$top() { return topPos; }
    @Override public int remnant$firstRow() { return totem$firstRow; }
    @Override public void remnant$firstRow(int row) { totem$firstRow = Math.clamp(row, 0, 255); }

    @Unique private boolean totem$observerReadOnly() {
        return (Object)this instanceof dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen observer
                && observer.totem$isObserverReadOnly();
    }

    @Unique
    private void totem$clearTransientSlotState() {
        hoveredSlot = null;
        quickCraftSlots.clear();
    }

    @Unique
    private List<Integer> totem$ordinaryBackpackSlots(Inventory inventory) {
        List<Integer> result = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (BackpackPanelContainer.isBackpack(inventory.getItem(slot))) {
                result.add(slot);
            }
        }
        return List.copyOf(result);
    }

    @Unique
    private int totem$leftBoundary() {
        if ((Object)this instanceof net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen<?> recipe) {
            var access=(SidebarAbstractRecipeBookScreenAccessor)recipe;
            if (access.remnant$getRecipeBookComponent().isVisible() && !access.remnant$getWidthTooNarrow()) return leftPos - 147;
        }
        return leftPos;
    }

    @Unique
    private int totem$panelColumns(int screenWidth) {
        int rightSpace = screenWidth - (leftPos + imageWidth) - totem$PANEL_GAP - 4;
        int leftSpace = totem$leftBoundary() - totem$PANEL_GAP - 4;
        int available = Math.max(rightSpace, leftSpace);
        return Math.max(0, Math.min(totem$PREFERRED_COLUMNS,
                (available - totem$PANEL_HORIZONTAL_PADDING) / totem$SLOT_SIZE));
    }

    @Unique
    private int totem$panelLeft(int screenWidth, int panelWidth) {
        int right = leftPos + imageWidth + totem$PANEL_GAP;
        if (right + panelWidth <= screenWidth - 4) {
            return right;
        }
        int left = totem$leftBoundary() - panelWidth - totem$PANEL_GAP;
        if (left >= 4) {
            return left;
        }
        return Math.max(4, screenWidth - panelWidth - 4);
    }

    @Unique
    private String totem$fitTitle(String title, int maxWidth) {
        if (font.width(title) <= maxWidth) {
            return title;
        }
        String ellipsis = "…";
        String shortened = title;
        while (!shortened.isEmpty() && font.width(shortened + ellipsis) > maxWidth) {
            int codePoints = shortened.codePointCount(0, shortened.length());
            shortened = codePoints <= 1 ? "" : shortened.substring(
                    0,
                    shortened.offsetByCodePoints(0, codePoints - 1)
            );
        }
        return shortened.isEmpty() ? "" : shortened + ellipsis;
    }

    @Unique
    private void totem$renderPanelBackground(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int width,
            int height
    ) {
        graphics.fill(left, top, left + width, top + height, totem$BACKGROUND);
        graphics.outline(left, top, width, height, totem$BORDER_OUTER);
        graphics.horizontalLine(left + 1, left + width - 2, top + 1, totem$BORDER_LIGHT);
        graphics.horizontalLine(left + 2, left + width - 3, top + 2, totem$BORDER_LIGHT);
        graphics.verticalLine(left + 1, top + 1, top + height - 2, totem$BORDER_LIGHT);
        graphics.verticalLine(left + 2, top + 2, top + height - 3, totem$BORDER_LIGHT);
        graphics.horizontalLine(left + 2, left + width - 2, top + height - 2, totem$BORDER_DARK);
        graphics.horizontalLine(left + 3, left + width - 3, top + height - 3, totem$BORDER_DARK);
        graphics.verticalLine(left + width - 2, top + 2, top + height - 2, totem$BORDER_DARK);
        graphics.verticalLine(left + width - 3, top + 3, top + height - 3, totem$BORDER_DARK);
    }
}
