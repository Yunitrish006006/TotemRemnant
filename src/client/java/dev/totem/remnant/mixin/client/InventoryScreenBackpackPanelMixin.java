package dev.totem.remnant.mixin.client;

import dev.totem.remnant.inventory.BackpackPanelMenuAccess;
import dev.totem.remnant.item.TieredBackpackItem;
import dev.totem.remnant.network.SelectBackpackPanelPayload;
import dev.totem.remnant.upgrade.BackpackCapacity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Places the InventoryMenu's real selected-backpack slots beside vanilla inventory. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenBackpackPanelMixin extends AbstractRecipeBookScreen<InventoryMenu> {
    @Unique private static final int totem$PREFERRED_COLUMNS = 9;
    @Unique private static final int totem$MIN_COLUMNS = 3;
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

    protected InventoryScreenBackpackPanelMixin(
            InventoryMenu menu,
            RecipeBookComponent<?> recipeBook,
            Inventory inventory,
            Component title
    ) {
        super(menu, recipeBook, inventory, title);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if ((Object) menu instanceof BackpackPanelMenuAccess access) {
            int start = access.totem$getBackpackPanelSlotStart();
            int activeSlots = access.totem$getBackpackPanel().activeSlotCount();
            for (int panelSlot = 0; panelSlot < activeSlots; panelSlot++) {
                var slot = menu.getSlot(start + panelSlot);
                double slotLeft = leftPos + slot.x;
                double slotTop = topPos + slot.y;
                if (slot.isActive()
                        && mouseX >= slotLeft && mouseX < slotLeft + 16
                        && mouseY >= slotTop && mouseY < slotTop + 16) {
                    return false;
                }
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void totem$prepareInteractiveBackpackPanel(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if (minecraft == null || minecraft.player == null
                || !((Object) menu instanceof BackpackPanelMenuAccess access)) {
            return;
        }
        Inventory inventory = minecraft.player.getInventory();
        List<Integer> backpacks = totem$ordinaryBackpackSlots(inventory);
        if (backpacks.isEmpty()) {
            totem$selectBackpack(access, -1);
            if (access.totem$layoutBackpackSlots(-10_000, -10_000, 1)) {
                totem$clearTransientSlotState();
            }
            return;
        }

        if (hoveredSlot != null && hoveredSlot.container == inventory
                && hoveredSlot.getItem().getItem() instanceof TieredBackpackItem) {
            totem$selectBackpack(access, hoveredSlot.getContainerSlot());
        }
        if (!backpacks.contains(totem$selectedInventorySlot)) {
            int menuSelection = access.totem$getBackpackPanel().selectedInventorySlot();
            totem$selectBackpack(
                    access,
                    backpacks.contains(menuSelection) ? menuSelection : backpacks.getFirst()
            );
        }

        ItemStack backpack = inventory.getItem(totem$selectedInventorySlot);
        if (!(backpack.getItem() instanceof TieredBackpackItem)) {
            return;
        }
        int columns = totem$panelColumns(graphics.guiWidth());
        int panelWidth = totem$PANEL_HORIZONTAL_PADDING + columns * totem$SLOT_SIZE;
        int storageSlots = BackpackCapacity.configuredSlots(backpack);
        int rows = (storageSlots + columns - 1) / columns;
        int panelHeight = totem$HEADER_HEIGHT + rows * totem$SLOT_SIZE
                + totem$PANEL_PADDING_BOTTOM;
        int panelLeft = totem$panelLeft(graphics.guiWidth(), panelWidth);
        int panelTop = Math.max(4, Math.min(
                topPos + (imageHeight - panelHeight) / 2,
                graphics.guiHeight() - panelHeight - 4
        ));

        if (access.totem$layoutBackpackSlots(
                panelLeft + 7 - leftPos,
                panelTop + totem$HEADER_HEIGHT - topPos,
                columns
        )) {
            totem$clearTransientSlotState();
        }

        totem$renderPanelBackground(graphics, panelLeft, panelTop, panelWidth, panelHeight);
        for (int slot = 0; slot < storageSlots; slot++) {
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
        if (inventorySlot == totem$selectedInventorySlot) {
            return;
        }
        totem$selectedInventorySlot = inventorySlot;
        access.totem$selectBackpackSlot(inventorySlot);
        if (ClientPlayNetworking.canSend(SelectBackpackPanelPayload.TYPE)) {
            ClientPlayNetworking.send(new SelectBackpackPanelPayload(inventorySlot));
        }
        totem$clearTransientSlotState();
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
            if (inventory.getItem(slot).getItem() instanceof TieredBackpackItem) {
                result.add(slot);
            }
        }
        return List.copyOf(result);
    }

    @Unique
    private int totem$panelColumns(int screenWidth) {
        int rightSpace = screenWidth - (leftPos + imageWidth) - totem$PANEL_GAP - 4;
        int leftSpace = leftPos - totem$PANEL_GAP - 4;
        int available = Math.max(rightSpace, leftSpace);
        return Math.max(totem$MIN_COLUMNS, Math.min(totem$PREFERRED_COLUMNS,
                (available - totem$PANEL_HORIZONTAL_PADDING) / totem$SLOT_SIZE));
    }

    @Unique
    private int totem$panelLeft(int screenWidth, int panelWidth) {
        int right = leftPos + imageWidth + totem$PANEL_GAP;
        if (right + panelWidth <= screenWidth - 4) {
            return right;
        }
        int left = leftPos - panelWidth - totem$PANEL_GAP;
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
