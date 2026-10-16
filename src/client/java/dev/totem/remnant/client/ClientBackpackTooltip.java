package dev.totem.remnant.client;

import dev.totem.remnant.item.BackpackTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Uses the native bundle slot sprites and integer geometry with backpack slot semantics. */
public final class ClientBackpackTooltip implements ClientTooltipComponent {
    private static final int COLUMNS = 4;
    private static final int SLOT_SIZE = 24;
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final Identifier SELECTED_BACK = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final Identifier SELECTED_FRONT = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
    private final BackpackTooltip contents;

    public ClientBackpackTooltip(BackpackTooltip contents) { this.contents = contents; }
    @Override public int getWidth(Font font) { return COLUMNS * SLOT_SIZE; }
    @Override public int getHeight(Font font) { return rows() * SLOT_SIZE + font.lineHeight + 4; }
    @Override public boolean showTooltipWithItemInHand() { return true; }
    private int rows() { return (contents.shownItems().size() + COLUMNS - 1) / COLUMNS; }

    @Override public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        int left = x + (width - getWidth(font)) / 2;
        var shown = contents.shownItems();
        for (int i = 0; i < shown.size(); i++) {
            int sx = left + i % COLUMNS * SLOT_SIZE;
            int sy = y + i / COLUMNS * SLOT_SIZE;
            boolean selected = contents.firstShownIndex() + i == contents.selectedIndex();
            var item = shown.get(i).create();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? SELECTED_BACK : SLOT, sx, sy, SLOT_SIZE, SLOT_SIZE);
            graphics.item(item, sx + 4, sy + 4, i);
            graphics.itemDecorations(font, item, sx + 4, sy + 4);
            if (selected) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTED_FRONT, sx, sy, SLOT_SIZE, SLOT_SIZE);
        }
        String position = (contents.selectedIndex() + 1) + " / " + contents.items().size();
        graphics.text(font, position, left + (getWidth(font) - font.width(position)) / 2,
                y + rows() * SLOT_SIZE + 2, 0xFFAAAAAA, false);
    }
}
