package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.screen.BackpackScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BackpackScreen extends HandledScreen<BackpackScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");

    public BackpackScreen(BackpackScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        int visibleRows = handler.getMaxVisibleRows();
        this.backgroundHeight = 114 + visibleRows * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        int visibleRows = handler.getMaxVisibleRows();

        // 繪製背包部分（可見行數）
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, visibleRows * 18 + 17);
        // 繪製玩家背包部分
        context.drawTexture(TEXTURE, x, y + visibleRows * 18 + 17, 0, 126, this.backgroundWidth, 96);

        // 繪製滾動條（如果需要）
        if (handler.canScroll()) {
            drawScrollbar(context, x, y, visibleRows);
        }
    }

    private void drawScrollbar(DrawContext context, int containerX, int containerY, int visibleRows) {
        int scrollTrackX = containerX + backgroundWidth - 6;
        int scrollTrackY = containerY + 18;
        int scrollTrackHeight = visibleRows * 18;

        // 滾動條軌道（深灰色背景）
        context.fill(scrollTrackX, scrollTrackY, scrollTrackX + 4, scrollTrackY + scrollTrackHeight, 0xFF3C3C3C);

        // 滾動條滑塊
        int maxScrollRow = handler.getMaxScrollRow();
        if (maxScrollRow > 0) {
            int thumbHeight = Math.max(8, scrollTrackHeight * visibleRows / handler.getRows());
            int scrollableHeight = scrollTrackHeight - thumbHeight;
            int thumbY = scrollTrackY + (int) (scrollableHeight * ((float) handler.getScrollRow() / maxScrollRow));

            // 滑塊（亮灰色）
            context.fill(scrollTrackX, thumbY, scrollTrackX + 4, thumbY + thumbHeight, 0xFFA0A0A0);
            // 滑塊邊框（更亮）
            context.fill(scrollTrackX, thumbY, scrollTrackX + 4, thumbY + 1, 0xFFC0C0C0);
            context.fill(scrollTrackX, thumbY + thumbHeight - 1, scrollTrackX + 4, thumbY + thumbHeight, 0xFF808080);
        }

        // 在標題旁顯示行數指示
        int currentTop = handler.getScrollRow() + 1;
        int currentBottom = Math.min(handler.getScrollRow() + visibleRows, handler.getRows());
        String scrollInfo = currentTop + "-" + currentBottom + "/" + handler.getRows();
        context.drawText(textRenderer, scrollInfo, containerX + backgroundWidth - textRenderer.getWidth(scrollInfo) - 8,
            containerY + 6, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (handler.canScroll()) {
            int newScrollRow = handler.getScrollRow() - (int) Math.signum(verticalAmount);
            handler.setScrollRow(newScrollRow);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
