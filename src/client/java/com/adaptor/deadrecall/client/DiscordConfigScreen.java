package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.DiscordConfigSyncPayload;
import com.adaptor.deadrecall.network.SaveDiscordConfigPayload;
import com.adaptor.deadrecall.network.ManageDiscordChannelPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class DiscordConfigScreen extends Screen {
    public static DiscordConfigScreen CURRENT = null;

    private static final int PANEL_MAX_WIDTH = 340;
    private static final int PANEL_MARGIN = 10;
    private static final int FIELD_GAP = 10;
    private static final int TOP_SECTION_Y = 72;
    private static final int CHANNEL_SECTION_Y = 190;
    private static final int CHANNEL_GRID_TOP = 256;
    private static final int CHANNEL_ROW_HEIGHT = 24;
    private static final int CHANNEL_ROW_GAP = 6;
    private static final int CHANNEL_DELETE_WIDTH = 40;
    private static final int CONTENT_TOP_PADDING = 20;
    private static final int CONTENT_BOTTOM_PADDING = 24;
    private static final int MAX_DISCORD_CHANNELS = 10;
    private static final String TRANSLATION_PREFIX = "message.deadrecall.discord_config.";

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

    private static final Component DESC_LINE1 = discordText("description_1");
    private static final Component DESC_LINE2 = discordText("description_2");
    private static final Component DESC_LINE3 = discordText("description_3");

    public DiscordConfigScreen() {
        super(discordText("title"));
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
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int halfWidth = getHalfFieldWidth();
        int y = TOP_SECTION_Y;

        this.workerUrlField = new EditBox(this.font, contentX, y + 18, contentWidth, 20, Component.literal("Worker URL"));
        this.workerUrlField.setHint(Component.literal("https://your-worker.workers.dev"));
        this.workerUrlField.setMaxLength(2048);
        this.addRenderableWidget(this.workerUrlField);

        this.apiKeyField = new EditBox(this.font, contentX, y + 56, contentWidth, 20, Component.literal("API Key"));
        this.apiKeyField.setHint(discordText("api_key_hint"));
        this.apiKeyField.setMaxLength(512);
        this.addRenderableWidget(this.apiKeyField);

        this.enabledButton = Button.builder(enabledButtonText(), button -> {
                    this.enabled = !this.enabled;
                    button.setMessage(enabledButtonText());
                })
                .bounds(contentX, y + 88, halfWidth, 20)
                .build();
        this.addRenderableWidget(this.enabledButton);

        this.saveButton = Button.builder(discordText("save"), button -> saveToServer())
                .bounds(contentX + halfWidth + FIELD_GAP, y + 88, halfWidth, 20)
                .build();
        this.addRenderableWidget(this.saveButton);

        // 頻道管理區域
        int channelStartY = CHANNEL_SECTION_Y;
        
        this.channelIdField = new EditBox(this.font, contentX, channelStartY + 18, halfWidth, 20, Component.literal("Channel ID"));
        this.channelIdField.setMaxLength(32);
        this.addRenderableWidget(this.channelIdField);

        this.channelNameField = new EditBox(this.font, contentX + halfWidth + FIELD_GAP, channelStartY + 18, halfWidth, 20, Component.literal("Channel Name"));
        this.channelNameField.setMaxLength(64);
        this.addRenderableWidget(this.channelNameField);

        this.addChannelButton = Button.builder(discordText("add_channel"), button -> addChannel())
                .bounds(contentX, channelStartY + 48, halfWidth, 20)
                .build();
        this.addRenderableWidget(this.addChannelButton);

        this.cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
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
        int centerX = this.width / 2;
        int panelX = getPanelX();
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int halfWidth = getHalfFieldWidth();
        int yOffset = -this.scrollOffset;
        int gridTop = CHANNEL_GRID_TOP + yOffset;
        int listX = contentX;

        // 頻道列背景先畫，避免蓋住後續的名稱文字與刪除按鈕。
        if (channels.isEmpty()) {
            drawChannelRowBackground(extractor, listX, gridTop + 18, contentWidth, CHANNEL_ROW_HEIGHT);
        } else {
            for (int i = 0; i < channels.size(); i++) {
                int rowY = gridTop + 18 + i * (CHANNEL_ROW_HEIGHT + CHANNEL_ROW_GAP);
                drawChannelRowBackground(extractor, listX, rowY, contentWidth, CHANNEL_ROW_HEIGHT);
            }
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // 標題與說明
        drawCenteredText(extractor, this.title.getString(), centerX, 10 + yOffset, 0xFFFFFFFF);
        drawCenteredText(extractor, DESC_LINE1.getString(), centerX, 28 + yOffset, 0xFFB8B8B8);
        drawCenteredText(extractor, DESC_LINE2.getString(), centerX, 40 + yOffset, 0xFFB8B8B8);
        drawCenteredText(extractor, DESC_LINE3.getString(), centerX, 52 + yOffset, 0xFFB8B8B8);

        // 基礎設定標籤
        extractor.text(this.font, discordText("basic_settings"), panelX + 10, TOP_SECTION_Y - 8 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Worker URL", contentX, TOP_SECTION_Y + 8 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "API Key", contentX, TOP_SECTION_Y + 46 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, discordText("api_key_note"), contentX, TOP_SECTION_Y + 114 + yOffset, 0xFFFFFFFF);

        // 頻道管理標籤
        extractor.text(this.font, discordText("channel_management"), panelX + 10, CHANNEL_SECTION_Y - 8 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Channel ID", contentX, CHANNEL_SECTION_Y + 8 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, "Channel Name", contentX + halfWidth + FIELD_GAP, CHANNEL_SECTION_Y + 8 + yOffset, 0xFFFFFFFF);
        extractor.text(this.font, discordText("channel_add_note"), contentX, CHANNEL_SECTION_Y + 76 + yOffset, 0xFFFFFFFF);

        // 頻道列表
        extractor.text(this.font, discordText("configured_channels"), listX + 5, gridTop + 3, 0xFFFFFFFF);

        if (channels.isEmpty()) {
            drawChannelRowText(extractor, listX, gridTop + 18, contentWidth, discordText("no_channels").getString(), "");
        } else {
            for (int i = 0; i < channels.size(); i++) {
                DiscordConfigSyncPayload.ChannelData ch = channels.get(i);
                int rowY = gridTop + 18 + i * (CHANNEL_ROW_HEIGHT + CHANNEL_ROW_GAP);
                drawChannelRowText(extractor, listX, rowY, contentWidth, ch.id(), ch.name());
            }
        }
    }

    private void updateLayout() {
        int baseOffset = -this.scrollOffset;
        int contentX = getContentX();
        int contentWidth = getContentWidth();
        int halfWidth = getHalfFieldWidth();
        int rightFieldX = contentX + halfWidth + FIELD_GAP;

        this.workerUrlField.setX(contentX);
        this.workerUrlField.setY(TOP_SECTION_Y + 18 + baseOffset);
        this.workerUrlField.setWidth(contentWidth);
        this.apiKeyField.setX(contentX);
        this.apiKeyField.setY(TOP_SECTION_Y + 56 + baseOffset);
        this.apiKeyField.setWidth(contentWidth);
        this.enabledButton.setX(contentX);
        this.enabledButton.setY(TOP_SECTION_Y + 88 + baseOffset);
        this.enabledButton.setWidth(halfWidth);
        this.saveButton.setX(rightFieldX);
        this.saveButton.setY(TOP_SECTION_Y + 88 + baseOffset);
        this.saveButton.setWidth(halfWidth);
        this.channelIdField.setX(contentX);
        this.channelIdField.setY(CHANNEL_SECTION_Y + 18 + baseOffset);
        this.channelIdField.setWidth(halfWidth);
        this.channelNameField.setX(rightFieldX);
        this.channelNameField.setY(CHANNEL_SECTION_Y + 18 + baseOffset);
        this.channelNameField.setWidth(halfWidth);
        this.addChannelButton.setX(contentX);
        this.addChannelButton.setY(CHANNEL_SECTION_Y + 48 + baseOffset);
        this.addChannelButton.setWidth(halfWidth);
        this.cancelButton.setX(this.width / 2 - 50);
        this.cancelButton.setY(20 + getContentHeight() - 28 - this.scrollOffset);

        rebuildChannelDeleteButtons();
        for (int i = 0; i < this.channelDeleteButtons.size() && i < this.channels.size(); i++) {
            int listX = contentX;
            int gridTop = CHANNEL_GRID_TOP + baseOffset;
            int cardY = gridTop + 18 + i * (CHANNEL_ROW_HEIGHT + CHANNEL_ROW_GAP);
            Button deleteButton = this.channelDeleteButtons.get(i);
            deleteButton.setX(listX + contentWidth - CHANNEL_DELETE_WIDTH - 4);
            deleteButton.setY(cardY + 3);
            deleteButton.setWidth(CHANNEL_DELETE_WIDTH);
        }
    }

    /**
     * 伺服器回傳設定後，填入欄位
     */
    public void applyServerConfig(boolean serverEnabled, String serverWorkerUrl, String serverApiKey) {
        this.enabled = serverEnabled;
        if (this.enabledButton != null) {
            this.enabledButton.setMessage(enabledButtonText());
        }
        if (this.workerUrlField != null) {
            this.workerUrlField.setValue(serverWorkerUrl);
        }
        if (this.apiKeyField != null) {
            this.apiKeyField.setValue("");
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
            Button deleteButton = Button.builder(discordText("delete"), button -> removeChannel(channelIndex))
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
                this.minecraft.player.sendSystemMessage(discordText("channel_id_empty").withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (!isValidChannelId(channelId)) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(discordText("channel_id_invalid").withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (this.channels.size() >= MAX_DISCORD_CHANNELS) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(discordText("channel_limit", MAX_DISCORD_CHANNELS).withStyle(ChatFormatting.RED));
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
                    this.minecraft.player.sendSystemMessage(discordText("channel_duplicate").withStyle(ChatFormatting.RED));
                }
                return;
            }
        }

        // 發送到伺服器
        ClientPlayNetworking.send(new ManageDiscordChannelPayload("add", channelId, channelName));
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
        return CHANNEL_GRID_TOP + getChannelGridHeight() + 48;
    }

    private int getChannelGridHeight() {
        int rows = Math.max(1, this.channels.size());
        return 18 + rows * (CHANNEL_ROW_HEIGHT + CHANNEL_ROW_GAP) + 12;
    }

    private void drawChannelRowBackground(GuiGraphicsExtractor extractor, int x, int y, int width, int height) {
        extractor.fill(x, y, x + width, y + height, 0xB01E1E1E);
    }

    private void drawChannelRowText(GuiGraphicsExtractor extractor, int x, int y, int width, String title, String subtitle) {
        int reservedDeleteWidth = subtitle.isEmpty() ? 0 : CHANNEL_DELETE_WIDTH + 10;
        int availableWidth = Math.max(40, width - 16 - reservedDeleteWidth);
        int idWidth = Math.max(70, availableWidth / 2);
        int nameX = x + 8 + idWidth + 8;
        int nameWidth = Math.max(40, width - (nameX - x) - reservedDeleteWidth - 8);
        String visibleId = subtitle.isEmpty()
                ? trimToWidth(title, availableWidth)
                : trimToWidth(discordText("channel_id_value", title).getString(), idWidth);
        String visibleName = subtitle.isEmpty() ? "" : trimToWidth(discordText("channel_name_value", subtitle).getString(), nameWidth);
        extractor.text(this.font, visibleId, x + 8, y + 8, 0xFFFFFFFF);
        if (!visibleName.isEmpty()) {
            extractor.text(this.font, visibleName, nameX, y + 8, 0xFFFFFFFF);
        }
    }

    private static MutableComponent discordText(String key, Object... args) {
        return Component.translatable(TRANSLATION_PREFIX + key, args);
    }

    private Component enabledButtonText() {
        return discordText("enabled_state", discordText(this.enabled ? "yes" : "no"));
    }

    private void drawCenteredText(GuiGraphicsExtractor extractor, String text, int centerX, int y, int color) {
        extractor.text(this.font, text, centerX - this.font.width(text) / 2, y, color);
    }

    private String trimToWidth(String text, int maxWidth) {
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
    }

    private int getPanelWidth() {
        return Math.max(180, Math.min(PANEL_MAX_WIDTH, this.width - PANEL_MARGIN * 2));
    }

    private int getPanelX() {
        return this.width / 2 - getPanelWidth() / 2;
    }

    private int getContentX() {
        return getPanelX() + 20;
    }

    private int getContentWidth() {
        return Math.max(120, getPanelWidth() - 40);
    }

    private int getHalfFieldWidth() {
        return Math.max(54, (getContentWidth() - FIELD_GAP) / 2);
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
    }

    private static boolean isValidChannelId(String channelId) {
        if (channelId.length() < 17 || channelId.length() > 20) {
            return false;
        }
        for (int i = 0; i < channelId.length(); i++) {
            if (!Character.isDigit(channelId.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
