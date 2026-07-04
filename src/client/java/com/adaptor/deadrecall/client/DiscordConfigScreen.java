package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.SaveDiscordConfigPayload;
import com.adaptor.deadrecall.network.ManageDiscordChannelPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class DiscordConfigScreen extends Screen {
    public static DiscordConfigScreen CURRENT = null;

    private static final int PANEL_WIDTH = 340;
    private static final int TOP_SECTION_Y = 34;
    private static final int CHANNEL_SECTION_Y = 152;
    private static final int CHANNEL_GRID_TOP = 218;
    private static final int CHANNEL_CARD_WIDTH = 145;
    private static final int CHANNEL_CARD_HEIGHT = 42;
    private static final int CHANNEL_CARD_GAP_X = 10;
    private static final int CHANNEL_CARD_GAP_Y = 8;
    private static final int CHANNEL_GRID_COLUMNS = 2;
    private static final int CONTENT_TOP_PADDING = 20;
    private static final int CONTENT_BOTTOM_PADDING = 24;

    private EditBox workerUrlField;
    private EditBox apiKeyField;
    private EditBox channelIdField;
    private EditBox channelNameField;
    private boolean enabled = false;
    private Button enabledButton;
    private Button saveButton;
    private Button addChannelButton;
    private Button cancelButton;
    private final List<Button> channelDeleteButtons = new ArrayList<>();
    private List<DiscordConfigSyncPayload.ChannelData> channels = new ArrayList<>();
    private int scrollOffset = 0;

    private static final Component DESC_LINE1 = Component.literal("§7此介面用於設定 Discord Bridge 功能：");
    private static final Component DESC_LINE2 = Component.literal("§7將 Minecraft 伺服器聊天訊息同步到 Discord 頻道。");
    private static final Component DESC_LINE3 = Component.literal("§7需要填入 Cloudflare Worker 的 URL 與 API Key。");

    public DiscordConfigScreen() {
        super(Component.literal("Discord Bridge 設定"));
        CURRENT = this;
    }

    @Override
    public void removed() {
        super.removed();
        if (CURRENT == this) {
            CURRENT = null;
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int y = TOP_SECTION_Y;

        this.workerUrlField = new EditBox(this.font, panelX + 20, y + 18, 300, 20, Component.literal("Worker URL"));
        this.workerUrlField.setHint(Component.literal("https://your-worker.workers.dev"));
        this.workerUrlField.setMaxLength(2048);
        this.addRenderableWidget(this.workerUrlField);

        this.apiKeyField = new EditBox(this.font, panelX + 20, y + 56, 300, 20, Component.literal("API Key"));
        this.apiKeyField.setHint(Component.literal("mc_ak_xxx"));
        this.apiKeyField.setMaxLength(512);
        this.addRenderableWidget(this.apiKeyField);

        this.enabledButton = Button.builder(Component.literal("啟用: 否"), button -> {
                    this.enabled = !this.enabled;
                    button.setMessage(Component.literal(this.enabled ? "啟用: 是" : "啟用: 否"));
                })
                .bounds(panelX + 20, y + 88, 145, 20)
                .build();
        this.addRenderableWidget(this.enabledButton);

        this.saveButton = Button.builder(Component.literal("儲存"), button -> saveToServer())
                .bounds(panelX + 175, y + 88, 145, 20)
                .build();
        this.addRenderableWidget(this.saveButton);

        // 頻道管理區域
        int channelStartY = CHANNEL_SECTION_Y;
        
        this.channelIdField = new EditBox(this.font, panelX + 20, channelStartY + 18, 145, 20, Component.literal("Channel ID"));
        this.channelIdField.setMaxLength(32);
        this.addRenderableWidget(this.channelIdField);

        this.channelNameField = new EditBox(this.font, panelX + 175, channelStartY + 18, 145, 20, Component.literal("Channel Name"));
        this.channelNameField.setMaxLength(64);
        this.addRenderableWidget(this.channelNameField);

        this.addChannelButton = Button.builder(Component.literal("添加頻道"), button -> addChannel())
                .bounds(panelX + 20, channelStartY + 48, 145, 20)
                .build();
        this.addRenderableWidget(this.addChannelButton);

        this.cancelButton = Button.builder(Component.literal("取消"), button -> this.onClose())
                .bounds(centerX - 50, 296, 100, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);

        this.setInitialFocus(this.workerUrlField);
        rebuildChannelDeleteButtons();
        updateLayout();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // 不使用預設的全畫面暗化背景，避免整個介面都被遮住
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int panelX = centerX - PANEL_WIDTH / 2;
        int yOffset = -this.scrollOffset;
        int gridTop = CHANNEL_GRID_TOP + yOffset;
        int contentHeight = getContentHeight();

        // 標題與說明
        drawCenteredText(extractor, this.title.getString(), centerX, 10 + yOffset, 0xFFFFFFFF);
        drawCenteredText(extractor, DESC_LINE1.getString().replace("§7", ""), centerX, 236 + yOffset, 0xFFFFFFFF);
        drawCenteredText(extractor, DESC_LINE2.getString().replace("§7", ""), centerX, 248 + yOffset, 0xFFFFFFFF);
        drawCenteredText(extractor, DESC_LINE3.getString().replace("§7", ""), centerX, 260 + yOffset, 0xFFFFFFFF);

        // 基礎設定標籤
        extractor.text(this.font, "基本設定", panelX + 10, 26 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Worker URL", panelX + 20, 46 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "API Key", panelX + 20, 84 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "填入 Worker 網址與 API Key 後按儲存", panelX + 20, 114 + yOffset, 0xFFFFFFFF);

        // 頻道管理標籤
        extractor.text(this.font, "頻道管理", panelX + 10, 144 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Channel ID", panelX + 20, 164 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Channel Name", panelX + 175, 164 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "新增後會即時顯示在下方雙欄清單", panelX + 20, 228 + yOffset, 0xFFFFFFFF);

        // 頻道列表
        int listX = panelX + 20;
        extractor.text(this.font, "已配置的頻道", listX + 5, gridTop + 3, 0xFFFFFFFF);

        if (channels.isEmpty()) {
            drawChannelCard(extractor, listX, gridTop + 18, 300, 32, "(無配置頻道)", "");
        } else {
            for (int i = 0; i < channels.size(); i++) {
                DiscordConfigSyncPayload.ChannelData ch = channels.get(i);
                int col = i % CHANNEL_GRID_COLUMNS;
                int row = i / CHANNEL_GRID_COLUMNS;
                int cardX = listX + col * (CHANNEL_CARD_WIDTH + CHANNEL_CARD_GAP_X);
                int cardY = gridTop + 18 + row * (CHANNEL_CARD_HEIGHT + CHANNEL_CARD_GAP_Y);
                drawChannelCard(extractor, cardX, cardY, CHANNEL_CARD_WIDTH, CHANNEL_CARD_HEIGHT, ch.id(), ch.name());
            }
        }
    }

    private void updateLayout() {
        int baseOffset = -this.scrollOffset;
        this.workerUrlField.setY(TOP_SECTION_Y + 18 + baseOffset);
        this.apiKeyField.setY(TOP_SECTION_Y + 56 + baseOffset);
        this.enabledButton.setY(TOP_SECTION_Y + 88 + baseOffset);
        this.saveButton.setY(TOP_SECTION_Y + 88 + baseOffset);
        this.channelIdField.setY(CHANNEL_SECTION_Y + 18 + baseOffset);
        this.channelNameField.setY(CHANNEL_SECTION_Y + 18 + baseOffset);
        this.addChannelButton.setY(CHANNEL_SECTION_Y + 48 + baseOffset);
        this.cancelButton.setY(20 + getContentHeight() - 28 - this.scrollOffset);

        rebuildChannelDeleteButtons();
        for (int i = 0; i < this.channelDeleteButtons.size() && i < this.channels.size(); i++) {
            int col = i % CHANNEL_GRID_COLUMNS;
            int row = i / CHANNEL_GRID_COLUMNS;
            int listX = this.width / 2 - PANEL_WIDTH / 2 + 20;
            int gridTop = CHANNEL_GRID_TOP + baseOffset;
            int cardX = listX + col * (CHANNEL_CARD_WIDTH + CHANNEL_CARD_GAP_X);
            int cardY = gridTop + 18 + row * (CHANNEL_CARD_HEIGHT + CHANNEL_CARD_GAP_Y);
            Button deleteButton = this.channelDeleteButtons.get(i);
            deleteButton.setX(cardX + CHANNEL_CARD_WIDTH - 42);
            deleteButton.setY(cardY + 11);
        }
    }

    /**
     * 伺服器回傳設定後，填入欄位
     */
    public void applyServerConfig(boolean serverEnabled, String serverWorkerUrl, String serverApiKey) {
        this.enabled = serverEnabled;
        if (this.enabledButton != null) {
            this.enabledButton.setMessage(Component.literal(this.enabled ? "啟用: 是" : "啟用: 否"));
        }
        if (this.workerUrlField != null) {
            this.workerUrlField.setValue(serverWorkerUrl);
        }
        if (this.apiKeyField != null) {
            this.apiKeyField.setValue(serverApiKey);
        }
    }

    /**
     * 伺服器回傳頻道列表
     */
    public void applyChannels(List<DiscordConfigSyncPayload.ChannelData> channelList) {
        this.channels = new ArrayList<>(channelList);
        this.scrollOffset = 0;
        rebuildChannelDeleteButtons();
        updateLayout();
    }

    private void rebuildChannelDeleteButtons() {
        for (Button button : this.channelDeleteButtons) {
            this.removeWidget(button);
        }
        this.channelDeleteButtons.clear();

        for (int i = 0; i < this.channels.size(); i++) {
            int channelIndex = i;
            DiscordConfigSyncPayload.ChannelData channel = this.channels.get(i);
            Button deleteButton = Button.builder(Component.literal("刪除"), button -> removeChannel(channelIndex))
                    .bounds(0, 0, 40, 18)
                    .build();
            this.channelDeleteButtons.add(deleteButton);
            this.addRenderableWidget(deleteButton);
        }
    }

    private void addChannel() {
        String channelId = this.channelIdField.getValue().trim();
        String channelName = this.channelNameField.getValue().trim();

        if (channelId.isEmpty()) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(Component.literal("§c頻道 ID 不能為空"));
            }
            return;
        }

        if (channelName.isEmpty()) {
            channelName = channelId;
        }

        // 檢查重複
        for (DiscordConfigSyncPayload.ChannelData ch : this.channels) {
            if (ch.id().equals(channelId)) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.sendSystemMessage(Component.literal("§c此頻道已存在"));
                }
                return;
            }
        }

        // 發送到伺服器
        ClientPlayNetworking.send(new ManageDiscordChannelPayload("add", channelId, channelName));
        
        // 本地更新
        this.channels.add(new DiscordConfigSyncPayload.ChannelData(channelId, channelName));
        this.scrollOffset = 0;
        updateLayout();
        this.channelIdField.setValue("");
        this.channelNameField.setValue("");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, getContentHeight() - (this.height - CONTENT_TOP_PADDING - CONTENT_BOTTOM_PADDING));
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (verticalAmount < 0) {
            this.scrollOffset = Math.min(maxScroll, this.scrollOffset + 12);
            updateLayout();
            return true;
        }
        if (verticalAmount > 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 12);
            updateLayout();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int getContentHeight() {
        return 20 + 210 + 18 + getChannelGridHeight() + 48;
    }

    private int getChannelGridHeight() {
        int rows = Math.max(1, (int) Math.ceil(this.channels.size() / 2.0));
        return 18 + rows * (CHANNEL_CARD_HEIGHT + CHANNEL_CARD_GAP_Y) + 12;
    }

    private void drawChannelCard(GuiGraphicsExtractor extractor, int x, int y, int width, int height, String title, String subtitle) {
        extractor.fill(x, y, x + width, y + height, 0xB01E1E1E);
        String visibleId = trimToWidth("ID: " + title, 95);
        String visibleName = trimToWidth("名稱: " + subtitle, 120);
        extractor.text(this.font, visibleId, x + 6, y + 11, 0xFFFFFFFF);
        extractor.text(this.font, visibleName, x + 104, y + 11, 0xFFB8B8B8);
    }

    private void drawCenteredText(GuiGraphicsExtractor extractor, String text, int centerX, int y, int color) {
        extractor.text(this.font, text, centerX - this.font.width(text) / 2, y, color);
    }

    private String trimToWidth(String text, int maxWidth) {
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
    }

    private void saveToServer() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        String workerUrl = this.workerUrlField.getValue().trim();
        String apiKey = this.apiKeyField.getValue().trim();
        ClientPlayNetworking.send(new SaveDiscordConfigPayload(this.enabled, workerUrl, apiKey));
        this.onClose();
    }

    private void removeChannel(int index) {
        if (index < 0 || index >= this.channels.size()) {
            return;
        }

        DiscordConfigSyncPayload.ChannelData channel = this.channels.get(index);
        ClientPlayNetworking.send(new ManageDiscordChannelPayload("remove", channel.id(), ""));
        this.channels.remove(index);
        this.scrollOffset = 0;
        rebuildChannelDeleteButtons();
        updateLayout();
    }
}
