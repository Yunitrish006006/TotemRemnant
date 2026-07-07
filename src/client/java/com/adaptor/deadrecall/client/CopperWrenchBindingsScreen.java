package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.CopperGolemOperationPayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import com.adaptor.deadrecall.network.SaveCopperGolemLlmConfigPayload;
import com.adaptor.deadrecall.network.TestCopperGolemLlmConnectionPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingLlmPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CopperWrenchBindingsScreen extends Screen {
    public static CopperWrenchBindingsScreen CURRENT = null;

    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 304;
    private static final int PANEL_PADDING = 12;
    private static final int HEADER_HEIGHT = 48;
    private static final int CARD_HEIGHT = 48;
    private static final int CARD_GAP = 4;
    private static final int PROMPT_SECTION_HEIGHT = 62;
    private static final int FOOTER_HEIGHT = 24;
    private static final int SCREEN_MARGIN = 6;

    private UUID golemId;
    private boolean running;
    private String llmApiUrl;
    private String llmApiKey;
    private String llmModel;
    private int llmActiveCount;
    private List<CopperWrenchBindingsPayload.BindingEntry> bindings;
    private Tab activeTab = Tab.BINDINGS;
    private Button bindingsTabButton;
    private Button llmTabButton;
    private Button operationButton;
    private Button saveApiButton;
    private Button testApiButton;
    private Button savePromptButton;
    private Button doneButton;
    private EditBox apiUrlField;
    private EditBox apiKeyField;
    private EditBox modelField;
    private EditBox promptField;
    private int selectedBindingIndex = -1;
    private int scrollOffset = 0;

    public CopperWrenchBindingsScreen(CopperWrenchBindingsPayload payload) {
        super(Component.translatable("container.deadrecall.copper_wrench.bindings"));
        this.golemId = payload.golemId();
        this.running = payload.running();
        this.llmApiUrl = payload.llmApiUrl();
        this.llmApiKey = payload.llmApiKey();
        this.llmModel = payload.llmModel();
        this.llmActiveCount = payload.llmActiveCount();
        this.bindings = new ArrayList<>(payload.bindings());
        CURRENT = this;
    }

    @Override
    public void removed() {
        super.removed();
        if (CURRENT == this) {
            CURRENT = null;
        }
    }

    public boolean isFor(UUID targetGolemId) {
        return this.golemId.equals(targetGolemId);
    }

    public void applyPayload(CopperWrenchBindingsPayload payload) {
        this.golemId = payload.golemId();
        this.running = payload.running();
        this.llmApiUrl = payload.llmApiUrl();
        this.llmApiKey = payload.llmApiKey();
        this.llmModel = payload.llmModel();
        this.llmActiveCount = payload.llmActiveCount();
        this.bindings = new ArrayList<>(payload.bindings());
        this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
        if (this.selectedBindingIndex >= this.bindings.size()) {
            this.selectedBindingIndex = this.bindings.isEmpty() ? -1 : this.bindings.size() - 1;
        }
        updateOperationButton();
        updateFields();
    }

    @Override
    protected void init() {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();

        this.operationButton = Button.builder(operationButtonText(), button -> toggleOperation())
                .bounds(panelX + panelWidth - PANEL_PADDING - 74, panelY + 7, 74, 18)
                .build();
        this.addRenderableWidget(this.operationButton);

        this.bindingsTabButton = Button.builder(Component.literal("箱子"), button -> setActiveTab(Tab.BINDINGS))
                .bounds(panelX + PANEL_PADDING, panelY + 26, 70, 18)
                .build();
        this.addRenderableWidget(this.bindingsTabButton);

        this.llmTabButton = Button.builder(Component.literal("LLM"), button -> setActiveTab(Tab.LLM))
                .bounds(panelX + PANEL_PADDING + 76, panelY + 26, 70, 18)
                .build();
        this.addRenderableWidget(this.llmTabButton);

        int promptY = promptSectionY();
        this.promptField = new EditBox(this.font, panelX + PANEL_PADDING + 74, promptY + 20, promptFieldWidth(), 18, Component.literal("LLM Prompt"));
        this.promptField.setMaxLength(2048);
        this.promptField.setHint(Component.literal("例：這個箱子只收礦物、金屬與礦石相關物品"));
        this.addRenderableWidget(this.promptField);

        this.savePromptButton = Button.builder(Component.literal("儲存 Prompt"), button -> saveSelectedPrompt())
                .bounds(panelX + panelWidth - PANEL_PADDING - 92, promptY + 19, 92, 20)
                .build();
        this.addRenderableWidget(this.savePromptButton);

        int apiX = panelX + PANEL_PADDING + 86;
        int apiY = apiControlsY();
        int fieldWidth = apiFieldWidth();
        this.apiUrlField = new EditBox(this.font, apiX, apiY, fieldWidth, 20, Component.literal("LLM API URL"));
        this.apiUrlField.setMaxLength(2048);
        this.apiUrlField.setHint(Component.literal("https://api.openai.com/v1/chat/completions"));
        this.addRenderableWidget(this.apiUrlField);

        this.apiKeyField = new EditBox(this.font, apiX, apiY + 36, fieldWidth, 20, Component.literal("LLM API Key"));
        this.apiKeyField.setMaxLength(512);
        this.apiKeyField.setHint(Component.literal("sk-..."));
        this.addRenderableWidget(this.apiKeyField);

        this.modelField = new EditBox(this.font, apiX, apiY + 72, fieldWidth, 20, Component.literal("LLM Model"));
        this.modelField.setMaxLength(256);
        this.modelField.setHint(Component.literal("gpt-4o-mini"));
        this.addRenderableWidget(this.modelField);

        this.saveApiButton = Button.builder(Component.literal("儲存這隻魁儡的 API 設定"), button -> saveApiConfig())
                .bounds(apiX, apiY + 106, 180, 20)
                .build();
        this.addRenderableWidget(this.saveApiButton);

        this.testApiButton = Button.builder(Component.literal("測試連線"), button -> testApiConnection())
                .bounds(apiX + 188, apiY + 106, 78, 20)
                .build();
        this.addRenderableWidget(this.testApiButton);

        this.doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(panelX + panelWidth / 2 - 45, panelY + panelHeight() - 21, 90, 18)
                .build();
        this.addRenderableWidget(this.doneButton);

        updateWidgetLayout();
        updateFields();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0xA0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        updateWidgetLayout();
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        extractor.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0181818);
        extractor.outline(panelX, panelY, panelWidth, panelHeight, 0xFF6A6A6A);
        extractor.text(this.font, this.title, panelX + PANEL_PADDING, panelY + 9, 0xFFFFFFFF);
        extractor.text(this.font, operationStatusText(), panelX + panelWidth - PANEL_PADDING - 162, panelY + 12, operationStatusColor());

        if (this.activeTab == Tab.BINDINGS) {
            drawBindingsTab(extractor, mouseX, mouseY);
        } else {
            drawLlmTab(extractor);
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.activeTab == Tab.BINDINGS) {
            int index = bindingIndexAt(event.x(), event.y());
            if (index >= 0) {
                this.selectedBindingIndex = index;
                CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(index);
                if (isLlmToggleAt(index, event.x(), event.y())) {
                    toggleBindingLlm(index);
                } else {
                    updateFields();
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.activeTab != Tab.BINDINGS) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (verticalAmount < 0) {
            this.scrollOffset = Math.min(maxScroll, this.scrollOffset + 14);
            return true;
        }
        if (verticalAmount > 0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 14);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void setActiveTab(Tab tab) {
        this.activeTab = tab;
        updateFields();
    }

    private void toggleOperation() {
        this.running = !this.running;
        updateOperationButton();
        if (ClientPlayNetworking.canSend(CopperGolemOperationPayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemOperationPayload(this.golemId, this.running));
        }
    }

    private void saveApiConfig() {
        this.llmApiUrl = this.apiUrlField.getValue().trim();
        this.llmApiKey = this.apiKeyField.getValue().trim();
        this.llmModel = this.modelField.getValue().trim();
        if (ClientPlayNetworking.canSend(SaveCopperGolemLlmConfigPayload.TYPE)) {
            ClientPlayNetworking.send(new SaveCopperGolemLlmConfigPayload(this.golemId, this.llmApiUrl, this.llmApiKey, this.llmModel));
        }
    }

    private void testApiConnection() {
        String apiUrl = this.apiUrlField.getValue().trim();
        String apiKey = this.apiKeyField.getValue().trim();
        String model = this.modelField.getValue().trim();
        if (ClientPlayNetworking.canSend(TestCopperGolemLlmConnectionPayload.TYPE)) {
            ClientPlayNetworking.send(new TestCopperGolemLlmConnectionPayload(apiUrl, apiKey, model));
        }
    }

    private void saveSelectedPrompt() {
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return;
        }

        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(this.selectedBindingIndex);
        updateBindingLlm(this.selectedBindingIndex, entry.llmEnabled(), this.promptField.getValue().trim());
    }

    private void toggleBindingLlm(int index) {
        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(index);
        String prompt = index == this.selectedBindingIndex && this.promptField != null ? this.promptField.getValue().trim() : entry.llmPrompt();
        updateBindingLlm(index, !entry.llmEnabled(), prompt);
    }

    private void updateBindingLlm(int index, boolean enabled, String prompt) {
        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(index);
        CopperWrenchBindingsPayload.BindingEntry updated = new CopperWrenchBindingsPayload.BindingEntry(
                entry.dimension(),
                entry.x(),
                entry.y(),
                entry.z(),
                entry.blockId(),
                entry.itemId(),
                entry.loaded(),
                entry.available(),
                enabled,
                prompt,
                entry.llmCachedItemIds(),
                entry.llmCachedTags(),
                entry.llmAllowedItemIds(),
                entry.llmDeniedItemIds(),
                entry.llmAllowedTags(),
                entry.llmDeniedTags()
        );
        this.bindings.set(index, updated);
        this.llmActiveCount += enabled == entry.llmEnabled() ? 0 : enabled ? 1 : -1;
        this.selectedBindingIndex = index;
        updateFields();

        if (ClientPlayNetworking.canSend(UpdateCopperGolemBindingLlmPayload.TYPE)) {
            ClientPlayNetworking.send(new UpdateCopperGolemBindingLlmPayload(
                    this.golemId,
                    entry.dimension(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    enabled,
                    prompt
            ));
        }
    }

    private void updateOperationButton() {
        if (this.operationButton != null) {
            this.operationButton.setMessage(operationButtonText());
        }
    }

    private void updateWidgetLayout() {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int promptY = promptSectionY();
        int apiX = panelX + PANEL_PADDING + 86;
        int apiY = apiControlsY();
        int apiFieldWidth = apiFieldWidth();
        int saveApiWidth = saveApiButtonWidth();
        int testApiWidth = testApiButtonWidth();

        if (this.operationButton != null) {
            this.operationButton.setX(panelX + panelWidth - PANEL_PADDING - 74);
            this.operationButton.setY(panelY + 7);
            this.operationButton.setWidth(74);
        }
        if (this.bindingsTabButton != null) {
            this.bindingsTabButton.setX(panelX + PANEL_PADDING);
            this.bindingsTabButton.setY(panelY + 26);
        }
        if (this.llmTabButton != null) {
            this.llmTabButton.setX(panelX + PANEL_PADDING + 76);
            this.llmTabButton.setY(panelY + 26);
        }
        if (this.promptField != null) {
            this.promptField.setX(panelX + PANEL_PADDING + 74);
            this.promptField.setY(promptY + 20);
            this.promptField.setWidth(promptFieldWidth());
        }
        if (this.savePromptButton != null) {
            this.savePromptButton.setX(panelX + panelWidth - PANEL_PADDING - 92);
            this.savePromptButton.setY(promptY + 19);
            this.savePromptButton.setWidth(92);
        }
        if (this.apiUrlField != null) {
            this.apiUrlField.setX(apiX);
            this.apiUrlField.setY(apiY);
            this.apiUrlField.setWidth(apiFieldWidth);
        }
        if (this.apiKeyField != null) {
            this.apiKeyField.setX(apiX);
            this.apiKeyField.setY(apiY + 36);
            this.apiKeyField.setWidth(apiFieldWidth);
        }
        if (this.modelField != null) {
            this.modelField.setX(apiX);
            this.modelField.setY(apiY + 72);
            this.modelField.setWidth(apiFieldWidth);
        }
        if (this.saveApiButton != null) {
            this.saveApiButton.setX(apiX);
            this.saveApiButton.setY(apiY + 106);
            this.saveApiButton.setWidth(saveApiWidth);
        }
        if (this.testApiButton != null) {
            this.testApiButton.setX(apiX + saveApiWidth + 8);
            this.testApiButton.setY(apiY + 106);
            this.testApiButton.setWidth(testApiWidth);
        }
        if (this.doneButton != null) {
            this.doneButton.setX(panelX + panelWidth / 2 - 45);
            this.doneButton.setY(panelY + panelHeight - 21);
        }
    }

    private void updateFields() {
        updateWidgetLayout();
        if (this.bindingsTabButton != null) {
            this.bindingsTabButton.setMessage(Component.literal(this.activeTab == Tab.BINDINGS ? "[箱子]" : "箱子"));
        }
        if (this.llmTabButton != null) {
            this.llmTabButton.setMessage(Component.literal(this.activeTab == Tab.LLM ? "[LLM]" : "LLM"));
        }

        boolean bindingsVisible = this.activeTab == Tab.BINDINGS;
        boolean llmVisible = this.activeTab == Tab.LLM;

        if (this.promptField != null) {
            this.promptField.visible = bindingsVisible;
            this.promptField.active = bindingsVisible && this.selectedBindingIndex >= 0 && this.selectedBindingIndex < this.bindings.size();
            this.promptField.setEditable(this.promptField.active);
            this.promptField.setValue(selectedBindingPrompt());
        }
        if (this.savePromptButton != null) {
            this.savePromptButton.visible = bindingsVisible;
            this.savePromptButton.active = this.selectedBindingIndex >= 0 && this.selectedBindingIndex < this.bindings.size();
        }

        if (this.apiUrlField != null) {
            this.apiUrlField.visible = llmVisible;
            this.apiUrlField.setValue(this.llmApiUrl == null ? "" : this.llmApiUrl);
        }
        if (this.apiKeyField != null) {
            this.apiKeyField.visible = llmVisible;
            this.apiKeyField.setValue(this.llmApiKey == null ? "" : this.llmApiKey);
        }
        if (this.modelField != null) {
            this.modelField.visible = llmVisible;
            this.modelField.setValue(this.llmModel == null ? "" : this.llmModel);
        }
        if (this.saveApiButton != null) {
            this.saveApiButton.visible = llmVisible;
        }
        if (this.testApiButton != null) {
            this.testApiButton.visible = llmVisible;
        }
    }

    private String selectedBindingPrompt() {
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return "";
        }
        return this.bindings.get(this.selectedBindingIndex).llmPrompt();
    }

    private void drawBindingsTab(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int panelX = panelX();
        int panelY = panelY();
        int listX = panelX + PANEL_PADDING;
        int listY = panelY + HEADER_HEIGHT + 8;
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int listHeight = getListHeight();

        extractor.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80101010);
        extractor.outline(listX, listY, listWidth, listHeight, 0xFF3A3A3A);

        if (this.bindings.isEmpty()) {
            extractor.centeredText(this.font, Component.translatable("message.deadrecall.copper_wrench.binding_list_empty"),
                    panelX + PANEL_WIDTH / 2, listY + listHeight / 2 - 4, 0xFFB8B8B8);
        } else {
            extractor.enableScissor(listX + 1, listY + 1, listX + listWidth - 1, listY + listHeight - 1);
            for (int i = 0; i < this.bindings.size(); i++) {
                int cardY = listY + 7 + i * (CARD_HEIGHT + CARD_GAP) - this.scrollOffset;
                if (cardY + CARD_HEIGHT < listY || cardY > listY + listHeight) {
                    continue;
                }
                drawBindingCard(extractor, this.bindings.get(i), i, listX + 8, cardY, listWidth - 16, mouseX, mouseY);
            }
            extractor.disableScissor();
        }

        if (getMaxScroll() > 0) {
            drawScrollBar(extractor, listX + listWidth - 6, listY + 4, listHeight - 8);
        }

        drawPromptSection(extractor, mouseX, mouseY);
    }

    private void drawPromptSection(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = panelX() + PANEL_PADDING;
        int y = promptSectionY();
        int width = panelWidth() - PANEL_PADDING * 2;
        extractor.fill(x, y, x + width, y + PROMPT_SECTION_HEIGHT - 4, 0x80101010);
        extractor.outline(x, y, width, PROMPT_SECTION_HEIGHT - 4, 0xFF3A3A3A);
        extractor.text(this.font, "箱子 Prompt", x + 8, y + 6, 0xFFFFFFFF);
        extractor.text(this.font, "啟用 LLM 箱子: " + this.llmActiveCount, x + width - 108, y + 6, 0xFFE0E0E0);
        extractor.text(this.font, trimToWidth(selectedBindingLabel(), 62), x + 8, y + 25, 0xFFB8B8B8);
        drawSelectedCacheItems(extractor, x + 8, y + 40, width - 16, mouseX, mouseY);
    }

    private void drawLlmTab(GuiGraphicsExtractor extractor) {
        int panelX = panelX();
        int panelY = panelY();
        int x = panelX + PANEL_PADDING;
        int y = panelY + HEADER_HEIGHT + 8;
        int width = panelWidth() - PANEL_PADDING * 2;
        int height = panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 20;
        int apiY = apiControlsY();
        extractor.fill(x, y, x + width, y + height, 0x80101010);
        extractor.outline(x, y, width, height, 0xFF3A3A3A);
        extractor.text(this.font, "這隻銅魁儡共用的 LLM API 設定", x + 10, y + 8, 0xFFFFFFFF);
        extractor.text(this.font, "每個箱子的 prompt 在「箱子」分頁設定", x + 10, y + 22, 0xFFB8B8B8);
        extractor.text(this.font, "API URL", x + 10, apiY + 6, 0xFFB8B8B8);
        extractor.text(this.font, "API Key", x + 10, apiY + 42, 0xFFB8B8B8);
        extractor.text(this.font, "Model", x + 10, apiY + 78, 0xFFB8B8B8);
        extractor.text(this.font, "目前啟用 LLM 的箱子: " + this.llmActiveCount, x + 10, apiY + 132, 0xFFE0E0E0);
    }

    private void drawBindingCard(GuiGraphicsExtractor extractor, CopperWrenchBindingsPayload.BindingEntry entry, int index,
                                 int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        boolean selected = index == this.selectedBindingIndex;
        int borderColor = selected ? 0xFFE2C15A : entry.available() ? 0xFF4C8A53 : entry.loaded() ? 0xFF9A4D4D : 0xFF777777;
        extractor.fill(x, y, x + width, y + CARD_HEIGHT, hovered || selected ? 0xC02A2A2A : 0xB0222222);
        extractor.outline(x, y, width, CARD_HEIGHT, borderColor);

        extractor.fill(x + 8, y + 7, x + 28, y + 27, 0xB0000000);
        extractor.item(iconStack(entry.itemId()), x + 10, y + 9);

        String title = (index + 1) + ". " + blockDisplayName(entry.blockId()).getString();
        extractor.text(this.font, trimToWidth(title, width - 176), x + 38, y + 5, 0xFFFFFFFF);
        extractor.text(this.font, entry.dimension(), x + 38, y + 18, 0xFFB8B8B8);
        extractor.text(this.font, trimToWidth("Prompt: " + emptyAsDash(entry.llmPrompt()), width - 154), x + 38, y + 33, entry.llmEnabled() ? 0xFFE0E0E0 : 0xFF909090);

        extractor.text(this.font, entry.x() + ", " + entry.y() + ", " + entry.z(), x + width - 132, y + 5, 0xFFE0E0E0);
        extractor.text(this.font, statusText(entry), x + width - 132, y + 18, statusColor(entry));
        extractor.text(this.font, "快取 " + entry.llmCachedItemIds() + "/" + entry.llmCachedTags(), x + width - 132, y + 33, 0xFFB8B8B8);

        int buttonX = x + width - 62;
        int buttonY = y + 28;
        extractor.fill(buttonX, buttonY, buttonX + 52, buttonY + 16, entry.llmEnabled() ? 0xFF326A3D : 0xFF4A4A4A);
        extractor.outline(buttonX, buttonY, 52, 16, entry.llmEnabled() ? 0xFF74D17B : 0xFF7A7A7A);
        extractor.centeredText(this.font, Component.literal(entry.llmEnabled() ? "LLM 開" : "LLM 關"), buttonX + 26, buttonY + 4, 0xFFFFFFFF);

        if (hovered) {
            extractor.setComponentTooltipForNextFrame(this.font, bindingTooltip(entry), mouseX, mouseY);
        }
    }

    private Component operationButtonText() {
        return Component.translatable(this.running
                ? "message.deadrecall.copper_wrench.action_stop"
                : "message.deadrecall.copper_wrench.action_start");
    }

    private Component operationStatusText() {
        return Component.translatable(this.running
                ? "message.deadrecall.copper_wrench.operation_running"
                : "message.deadrecall.copper_wrench.operation_stopped");
    }

    private int operationStatusColor() {
        return this.running ? 0xFF64D26D : 0xFFFF6B6B;
    }

    private String selectedBindingLabel() {
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return "Prompt";
        }
        return "箱子 #" + (this.selectedBindingIndex + 1);
    }

    private void drawSelectedCacheItems(GuiGraphicsExtractor extractor, int x, int y, int width, int mouseX, int mouseY) {
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            drawEmptyCacheSlots(extractor, x, y, width);
            return;
        }

        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(this.selectedBindingIndex);
        int gap = 8;
        int groupWidth = (width - gap) / 2;
        drawCacheItemGroup(extractor, entry.llmAllowedItemIds(), x, y, groupWidth, 0xFF4C8A53, "接受", mouseX, mouseY);
        drawCacheItemGroup(extractor, entry.llmDeniedItemIds(), x + groupWidth + gap, y, groupWidth, 0xFF9A4D4D, "拒絕", mouseX, mouseY);
    }

    private void drawEmptyCacheSlots(GuiGraphicsExtractor extractor, int x, int y, int width) {
        int gap = 8;
        int groupWidth = (width - gap) / 2;
        drawCacheItemGroup(extractor, List.of(), x, y, groupWidth, 0xFF4C8A53, "接受", -1, -1);
        drawCacheItemGroup(extractor, List.of(), x + groupWidth + gap, y, groupWidth, 0xFF9A4D4D, "拒絕", -1, -1);
    }

    private void drawCacheItemGroup(GuiGraphicsExtractor extractor, List<String> itemIds, int x, int y, int width, int color, String label, int mouseX, int mouseY) {
        extractor.outline(x, y, width, 18, color);
        extractor.text(this.font, label, x + 4, y + 5, color);

        int iconX = x + 28;
        int maxIcons = Math.max(0, (width - 34) / 18);
        int shown = Math.min(itemIds.size(), maxIcons);
        for (int i = 0; i < shown; i++) {
            String itemId = itemIds.get(i);
            int slotX = iconX + i * 18;
            extractor.fill(slotX, y + 1, slotX + 17, y + 18, 0xA0000000);
            extractor.outline(slotX, y + 1, 17, 17, color);
            extractor.item(iconStack(itemId), slotX + 1, y + 2);
            if (mouseX >= slotX && mouseX <= slotX + 17 && mouseY >= y + 1 && mouseY <= y + 18) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal(label + "物品"),
                        Component.literal(itemId)
                ), mouseX, mouseY);
            }
        }

        int remaining = itemIds.size() - shown;
        if (remaining > 0) {
            extractor.text(this.font, "+" + remaining, iconX + shown * 18 + 2, y + 5, 0xFFE0E0E0);
        }
    }

    private List<Component> bindingTooltip(CopperWrenchBindingsPayload.BindingEntry entry) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(blockDisplayName(entry.blockId()));
        tooltip.add(Component.literal("ID: " + entry.blockId()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_dimension", entry.dimension()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_position", entry.x(), entry.y(), entry.z()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_status", Component.literal(statusText(entry))));
        tooltip.add(Component.literal("LLM: " + (entry.llmEnabled() ? "啟用" : "停用")));
        tooltip.add(Component.literal("快取物品: " + entry.llmCachedItemIds() + " / 快取 Tag: " + entry.llmCachedTags()));
        tooltip.add(Component.literal("允許 Tag: " + joinCacheValues(entry.llmAllowedTags(), 4)));
        tooltip.add(Component.literal("拒絕 Tag: " + joinCacheValues(entry.llmDeniedTags(), 4)));
        return tooltip;
    }

    private String joinCacheValues(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "-";
        }

        int count = Math.min(values.size(), limit);
        String joined = String.join(", ", values.subList(0, count));
        if (values.size() > limit) {
            joined += " +" + (values.size() - limit);
        }
        return joined;
    }

    private boolean isLlmToggleAt(int index, double mouseX, double mouseY) {
        int listX = panelX() + PANEL_PADDING;
        int listY = panelY() + HEADER_HEIGHT + 8;
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int cardX = listX + 8;
        int cardY = listY + 7 + index * (CARD_HEIGHT + CARD_GAP) - this.scrollOffset;
        int buttonX = cardX + listWidth - 16 - 62;
        int buttonY = cardY + 28;
        return mouseX >= buttonX && mouseX <= buttonX + 52 && mouseY >= buttonY && mouseY <= buttonY + 16;
    }

    private int bindingIndexAt(double mouseX, double mouseY) {
        int listX = panelX() + PANEL_PADDING;
        int listY = panelY() + HEADER_HEIGHT + 8;
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int listHeight = getListHeight();
        if (mouseX < listX + 1 || mouseX > listX + listWidth - 1 || mouseY < listY + 1 || mouseY > listY + listHeight - 1) {
            return -1;
        }

        int relativeY = (int) mouseY - listY - 7 + this.scrollOffset;
        if (relativeY < 0) {
            return -1;
        }

        int index = relativeY / (CARD_HEIGHT + CARD_GAP);
        int yInCard = relativeY % (CARD_HEIGHT + CARD_GAP);
        if (index < 0 || index >= this.bindings.size() || yInCard > CARD_HEIGHT) {
            return -1;
        }
        return index;
    }

    private void drawScrollBar(GuiGraphicsExtractor extractor, int x, int y, int height) {
        int contentHeight = getContentHeight();
        int listHeight = getListHeight();
        int thumbHeight = Math.max(18, height * listHeight / Math.max(listHeight, contentHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + thumbTravel * this.scrollOffset / Math.max(1, getMaxScroll());
        extractor.fill(x, y, x + 3, y + height, 0x80333333);
        extractor.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xFF9A9A9A);
    }

    private ItemStack iconStack(String itemId) {
        Identifier identifier = Identifier.tryParse(itemId);
        Item item = identifier == null
                ? Items.BARRIER
                : BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.BARRIER);
        if (item == Items.AIR) {
            item = Items.BARRIER;
        }
        return new ItemStack(item);
    }

    private Component blockDisplayName(String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            return Component.literal(blockId);
        }
        return BuiltInRegistries.BLOCK.getOptional(identifier)
                .map(block -> block.getName())
                .orElse(Component.literal(blockId));
    }

    private String statusText(CopperWrenchBindingsPayload.BindingEntry entry) {
        if (!entry.loaded()) {
            return Component.translatable("message.deadrecall.copper_wrench.binding_status_unloaded").getString();
        }
        return Component.translatable(entry.available()
                ? "message.deadrecall.copper_wrench.binding_status_available"
                : "message.deadrecall.copper_wrench.binding_status_unavailable").getString();
    }

    private int statusColor(CopperWrenchBindingsPayload.BindingEntry entry) {
        if (!entry.loaded()) {
            return 0xFFB8B8B8;
        }
        return entry.available() ? 0xFF64D26D : 0xFFFF6B6B;
    }

    private int getListHeight() {
        return Math.max(24, panelHeight() - HEADER_HEIGHT - PROMPT_SECTION_HEIGHT - FOOTER_HEIGHT - 16);
    }

    private int getContentHeight() {
        return this.bindings.size() * (CARD_HEIGHT + CARD_GAP) + 10;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - getListHeight());
    }

    private int panelX() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int panelY() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int promptSectionY() {
        return panelY() + panelHeight() - FOOTER_HEIGHT - PROMPT_SECTION_HEIGHT;
    }

    private int panelWidth() {
        int availableWidth = Math.max(1, this.width - SCREEN_MARGIN * 2);
        return Math.min(PANEL_WIDTH, availableWidth);
    }

    private int panelHeight() {
        int availableHeight = Math.max(1, this.height - SCREEN_MARGIN * 2);
        return Math.min(PANEL_HEIGHT, availableHeight);
    }

    private int promptFieldWidth() {
        return Math.max(80, panelWidth() - PANEL_PADDING * 2 - 74 - 100);
    }

    private int apiFieldWidth() {
        return Math.max(120, Math.min(300, panelWidth() - PANEL_PADDING * 2 - 98));
    }

    private int apiControlsY() {
        int availableOffset = panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 154;
        int compactOffset = Math.max(8, Math.min(38, availableOffset));
        return panelY() + HEADER_HEIGHT + compactOffset;
    }

    private int saveApiButtonWidth() {
        return Math.max(110, Math.min(180, apiFieldWidth() - 86));
    }

    private int testApiButtonWidth() {
        int buttonX = panelX() + PANEL_PADDING + 86 + saveApiButtonWidth() + 8;
        int right = panelX() + panelWidth() - PANEL_PADDING;
        return Math.max(54, Math.min(78, right - buttonX));
    }

    private String trimToWidth(String text, int maxWidth) {
        return this.font.plainSubstrByWidth(text, Math.max(0, maxWidth));
    }

    private String emptyAsDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private enum Tab {
        BINDINGS,
        LLM
    }
}
