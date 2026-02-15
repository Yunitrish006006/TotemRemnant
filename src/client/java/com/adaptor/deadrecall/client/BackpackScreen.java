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
        int rows = handler.getTier().getRows();
        this.backgroundHeight = 114 + rows * 18; // 背包行數 + 玩家背包
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        int rows = handler.getTier().getRows();

        // 繪製背包部分（動態行數）
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, rows * 18 + 17);
        // 繪製玩家背包部分
        context.drawTexture(TEXTURE, x, y + rows * 18 + 17, 0, 126, this.backgroundWidth, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}

