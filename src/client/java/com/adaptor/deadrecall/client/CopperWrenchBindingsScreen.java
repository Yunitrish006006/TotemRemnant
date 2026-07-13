package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.CopperGolemOperationPayload;
import com.adaptor.deadrecall.network.CopperGolemFuelSlotPayload;
import com.adaptor.deadrecall.network.CopperGolemGatheringSlotPayload;
import com.adaptor.deadrecall.network.CopperGolemGatheringTargetPayload;
import com.adaptor.deadrecall.network.CopperGolemModePayload;
import com.adaptor.deadrecall.network.CopperWrenchBindingsPayload;
import com.adaptor.deadrecall.network.SaveCopperGolemLlmConfigPayload;
import com.adaptor.deadrecall.network.TestCopperGolemLlmConnectionPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingCachePayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemBindingLlmPayload;
import com.adaptor.deadrecall.network.UpdateCopperGolemGatheringLlmPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CopperWrenchBindingsScreen extends Screen {
    public static CopperWrenchBindingsScreen CURRENT = null;

    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 304;
    private static final int PANEL_PADDING = 12;
    private static final int HEADER_HEIGHT = 48;
    private static final int SOURCE_SECTION_HEIGHT = 58;
    private static final int TARGET_CARD_COMPACT_HEIGHT = SOURCE_SECTION_HEIGHT;
    private static final int TARGET_CARD_EXPANDED_HEIGHT = 100;
    private static final int CARD_GAP = 4;
    private static final int FOOTER_HEIGHT = 24;
    private static final int SCREEN_MARGIN = 6;

    private UUID golemId;
    private int revision;
    private boolean running;
    private String mode;
    private String activity;
    private String fuelItemId;
    private int fuelCount;
    private int fuelTicks;
    private String gatheringToolItemId;
    private int gatheringToolCount;
    private int gatheringToolDamage;
    private int gatheringToolMaxDamage;
    private String gatheringStorageItemId;
    private int gatheringStorageCount;
    private String llmApiUrl;
    private String llmApiKey;
    private String llmModel;
    private int llmActiveCount;
    private CopperWrenchBindingsPayload.BindingEntry sourceContainer;
    private CopperWrenchBindingsPayload.GatheringAreaEntry gatheringArea;
    private List<String> gatheringManualTargets;
    private boolean gatheringLlmEnabled;
    private String gatheringLlmPrompt;
    private int gatheringLlmCachedBlockIds;
    private int gatheringLlmCachedTags;
    private List<String> gatheringLlmAllowedBlockIds;
    private List<String> gatheringLlmDeniedBlockIds;
    private List<String> gatheringLlmAllowedTags;
    private List<String> gatheringLlmDeniedTags;
    private List<CopperWrenchBindingsPayload.BindingEntry> bindings;
    private Tab activeTab = Tab.BINDINGS;
    private Button bindingsTabButton;
    private Button llmTabButton;
    private Button modeButton;
    private Button operationButton;
    private Button saveApiButton;
    private Button testApiButton;
    private Button savePromptButton;
    private Button gatheringLlmToggleButton;
    private Button doneButton;
    private EditBox apiUrlField;
    private EditBox apiKeyField;
    private EditBox modelField;
    private EditBox promptField;
    private int selectedBindingIndex = -1;
    private int scrollOffset = 0;
    private String promptFieldContext = "";

    public CopperWrenchBindingsScreen(CopperWrenchBindingsPayload payload) {
        super(Component.translatable("container.deadrecall.copper_wrench.bindings"));
        this.golemId = payload.golemId();
        this.revision = payload.revision();
        this.running = payload.running();
        this.mode = payload.mode();
        this.activity = payload.activity();
        this.fuelItemId = payload.fuelItemId();
        this.fuelCount = payload.fuelCount();
        this.fuelTicks = payload.fuelTicks();
        this.gatheringToolItemId = payload.gatheringToolItemId();
        this.gatheringToolCount = payload.gatheringToolCount();
        this.gatheringToolDamage = payload.gatheringToolDamage();
        this.gatheringToolMaxDamage = payload.gatheringToolMaxDamage();
        this.gatheringStorageItemId = payload.gatheringStorageItemId();
        this.gatheringStorageCount = payload.gatheringStorageCount();
        this.llmApiUrl = payload.llmApiUrl();
        this.llmApiKey = payload.llmApiKey();
        this.llmModel = payload.llmModel();
        this.llmActiveCount = payload.llmActiveCount();
        this.sourceContainer = payload.sourceContainer();
        this.gatheringArea = payload.gatheringArea();
        this.gatheringManualTargets = new ArrayList<>(payload.gatheringManualTargets());
        this.gatheringLlmEnabled = payload.gatheringLlmEnabled();
        this.gatheringLlmPrompt = payload.gatheringLlmPrompt();
        this.gatheringLlmCachedBlockIds = payload.gatheringLlmCachedBlockIds();
        this.gatheringLlmCachedTags = payload.gatheringLlmCachedTags();
        this.gatheringLlmAllowedBlockIds = new ArrayList<>(payload.gatheringLlmAllowedBlockIds());
        this.gatheringLlmDeniedBlockIds = new ArrayList<>(payload.gatheringLlmDeniedBlockIds());
        this.gatheringLlmAllowedTags = new ArrayList<>(payload.gatheringLlmAllowedTags());
        this.gatheringLlmDeniedTags = new ArrayList<>(payload.gatheringLlmDeniedTags());
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
        this.revision = payload.revision();
        this.running = payload.running();
        this.mode = payload.mode();
        this.activity = payload.activity();
        this.fuelItemId = payload.fuelItemId();
        this.fuelCount = payload.fuelCount();
        this.fuelTicks = payload.fuelTicks();
        this.gatheringToolItemId = payload.gatheringToolItemId();
        this.gatheringToolCount = payload.gatheringToolCount();
        this.gatheringToolDamage = payload.gatheringToolDamage();
        this.gatheringToolMaxDamage = payload.gatheringToolMaxDamage();
        this.gatheringStorageItemId = payload.gatheringStorageItemId();
        this.gatheringStorageCount = payload.gatheringStorageCount();
        this.llmApiUrl = payload.llmApiUrl();
        this.llmApiKey = payload.llmApiKey();
        this.llmModel = payload.llmModel();
        this.llmActiveCount = payload.llmActiveCount();
        this.sourceContainer = payload.sourceContainer();
        this.gatheringArea = payload.gatheringArea();
        this.gatheringManualTargets = new ArrayList<>(payload.gatheringManualTargets());
        this.gatheringLlmEnabled = payload.gatheringLlmEnabled();
        this.gatheringLlmPrompt = payload.gatheringLlmPrompt();
        this.gatheringLlmCachedBlockIds = payload.gatheringLlmCachedBlockIds();
        this.gatheringLlmCachedTags = payload.gatheringLlmCachedTags();
        this.gatheringLlmAllowedBlockIds = new ArrayList<>(payload.gatheringLlmAllowedBlockIds());
        this.gatheringLlmDeniedBlockIds = new ArrayList<>(payload.gatheringLlmDeniedBlockIds());
        this.gatheringLlmAllowedTags = new ArrayList<>(payload.gatheringLlmAllowedTags());
        this.gatheringLlmDeniedTags = new ArrayList<>(payload.gatheringLlmDeniedTags());
        this.bindings = new ArrayList<>(payload.bindings());
        this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
        if (this.selectedBindingIndex >= this.bindings.size()) {
            this.selectedBindingIndex = this.bindings.isEmpty() ? -1 : this.bindings.size() - 1;
        }
        ensureSelectedBindingVisible();
        updateOperationButton();
        updateModeButton();
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

        this.modeButton = Button.builder(modeButtonText(), button -> switchMode())
                .bounds(panelX + panelWidth - PANEL_PADDING - 158, panelY + 7, 78, 18)
                .build();
        this.addRenderableWidget(this.modeButton);

        this.bindingsTabButton = Button.builder(Component.literal("箱子"), button -> setActiveTab(Tab.BINDINGS))
                .bounds(panelX + PANEL_PADDING, panelY + 26, 70, 18)
                .build();
        this.addRenderableWidget(this.bindingsTabButton);

        this.llmTabButton = Button.builder(Component.literal("LLM"), button -> setActiveTab(Tab.LLM))
                .bounds(panelX + PANEL_PADDING + 76, panelY + 26, 70, 18)
                .build();
        this.addRenderableWidget(this.llmTabButton);

        this.promptField = new EditBox(this.font, promptEditorX(), promptEditorY(), promptFieldWidth(), 18, Component.literal("LLM Prompt"));
        this.promptField.setMaxLength(2048);
        this.promptField.setHint(Component.literal("例：這個箱子只收礦物、金屬與礦石相關物品"));
        this.addRenderableWidget(this.promptField);

        this.savePromptButton = Button.builder(Component.literal("儲存"), button -> saveSelectedPrompt())
                .bounds(promptSaveButtonX(), promptSaveButtonY(), promptSaveButtonWidth(), 18)
                .build();
        this.addRenderableWidget(this.savePromptButton);

        this.gatheringLlmToggleButton = Button.builder(gatheringLlmToggleText(), button -> toggleGatheringLlm())
                .bounds(gatheringLlmToggleX(), gatheringLlmToggleY(), 62, 18)
                .build();
        this.addRenderableWidget(this.gatheringLlmToggleButton);

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
        extractor.text(this.font, operationStatusText(), panelX + PANEL_PADDING + 154, panelY + 31, operationStatusColor());
        drawSourceIcon(extractor, mouseX, mouseY);
        drawFuelSlot(extractor, mouseX, mouseY);

        if (this.activeTab == Tab.BINDINGS) {
            if (isGatheringMode()) {
                drawGatheringTab(extractor, mouseX, mouseY);
            } else {
                drawSortingTab(extractor, mouseX, mouseY);
            }
        } else {
            drawLlmTab(extractor);
        }

        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isFuelSlotAt(event.x(), event.y())) {
            clickFuelSlot(event.button());
            return true;
        }

        if (isPromptWidgetAt(event.x(), event.y())) {
            return super.mouseClicked(event, doubleClick);
        }

        if (this.activeTab == Tab.BINDINGS && isGatheringMode()) {
            if (isGatheringToolSlotAt(event.x(), event.y())) {
                clickGatheringSlot(CopperGolemGatheringSlotPayload.Slot.TOOL, event.button());
                return true;
            }
            if (isGatheringStorageSlotAt(event.x(), event.y())) {
                clickGatheringSlot(CopperGolemGatheringSlotPayload.Slot.STORAGE, event.button());
                return true;
            }
            GatheringTargetHit targetHit = gatheringTargetHitAt(event.x(), event.y());
            if (event.button() == 1 && targetHit != null) {
                removeGatheringTarget(targetHit);
                return true;
            }
        }

        if (this.activeTab == Tab.BINDINGS && !isGatheringMode()) {
            int index = bindingIndexAt(event.x(), event.y());
            if (index >= 0) {
                this.selectedBindingIndex = index;
                ensureSelectedBindingVisible();
                CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(index);
                CachePreviewHit cacheHit = cachePreviewHitAt(index, event.x(), event.y());
                if (event.button() == 1 && cacheHit != null) {
                    moveCachePreviewEntry(cacheHit);
                } else if (isLlmToggleAt(index, event.x(), event.y())) {
                    toggleBindingLlm(index);
                } else {
                    updateFields();
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void clickFuelSlot(int button) {
        CopperGolemFuelSlotPayload.Action action = button == 1
                ? CopperGolemFuelSlotPayload.Action.TAKE_ALL
                : CopperGolemFuelSlotPayload.Action.INSERT_MAIN_HAND;
        if (ClientPlayNetworking.canSend(CopperGolemFuelSlotPayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemFuelSlotPayload(this.golemId, action, this.revision));
        }
    }

    private void clickGatheringSlot(CopperGolemGatheringSlotPayload.Slot slot, int button) {
        CopperGolemGatheringSlotPayload.Action action = button == 1
                ? CopperGolemGatheringSlotPayload.Action.TAKE_ALL
                : CopperGolemGatheringSlotPayload.Action.INSERT_MAIN_HAND;
        if (ClientPlayNetworking.canSend(CopperGolemGatheringSlotPayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemGatheringSlotPayload(this.golemId, slot, action, this.revision));
        }
    }

    private void removeGatheringTarget(GatheringTargetHit hit) {
        removeGatheringTargetLocally(hit);
        if (ClientPlayNetworking.canSend(CopperGolemGatheringTargetPayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemGatheringTargetPayload(
                    this.golemId,
                    hit.value(),
                    hit.tag(),
                    hit.targetSet(),
                    CopperGolemGatheringTargetPayload.Action.REMOVE,
                    this.revision));
        }
    }

    private void removeGatheringTargetLocally(GatheringTargetHit hit) {
        if (hit.targetSet() == CopperGolemGatheringTargetPayload.TargetSet.MANUAL) {
            this.gatheringManualTargets.remove(hit.value());
        } else if (hit.targetSet() == CopperGolemGatheringTargetPayload.TargetSet.ALLOWED) {
            (hit.tag() ? this.gatheringLlmAllowedTags : this.gatheringLlmAllowedBlockIds).remove(hit.value());
        } else if (hit.targetSet() == CopperGolemGatheringTargetPayload.TargetSet.DENIED) {
            (hit.tag() ? this.gatheringLlmDeniedTags : this.gatheringLlmDeniedBlockIds).remove(hit.value());
        }
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
        this.activity = this.running ? (hasFuelAvailable() ? "searching" : "blocked_no_fuel") : "stopped";
        updateOperationButton();
        if (ClientPlayNetworking.canSend(CopperGolemOperationPayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemOperationPayload(this.golemId, this.running, this.revision));
        }
    }

    private void switchMode() {
        String nextMode = "gathering".equals(normalizedMode()) ? "sorting" : "gathering";
        if (ClientPlayNetworking.canSend(CopperGolemModePayload.TYPE)) {
            ClientPlayNetworking.send(new CopperGolemModePayload(this.golemId, nextMode, this.revision));
        }
    }

    private void saveApiConfig() {
        this.llmApiUrl = this.apiUrlField.getValue().trim();
        this.llmApiKey = this.apiKeyField.getValue().trim();
        this.llmModel = this.modelField.getValue().trim();
        if (ClientPlayNetworking.canSend(SaveCopperGolemLlmConfigPayload.TYPE)) {
            ClientPlayNetworking.send(new SaveCopperGolemLlmConfigPayload(this.golemId, this.llmApiUrl, this.llmApiKey, this.llmModel, this.revision));
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
        if (isGatheringMode()) {
            saveGatheringPrompt();
            return;
        }

        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return;
        }

        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(this.selectedBindingIndex);
        updateBindingLlm(this.selectedBindingIndex, entry.llmEnabled(), this.promptField.getValue().trim());
    }

    private void toggleBindingLlm(int index) {
        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(index);
        String prompt = index == this.selectedBindingIndex && this.promptField != null && this.promptField.visible
                ? this.promptField.getValue().trim()
                : entry.llmPrompt();
        updateBindingLlm(index, !entry.llmEnabled(), prompt);
    }

    private void moveCachePreviewEntry(CachePreviewHit hit) {
        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(hit.bindingIndex());
        boolean moveToAllowed = !hit.acceptedSide();

        List<String> allowedItemIds = new ArrayList<>(entry.llmAllowedItemIds());
        List<String> deniedItemIds = new ArrayList<>(entry.llmDeniedItemIds());
        List<String> allowedTags = new ArrayList<>(entry.llmAllowedTags());
        List<String> deniedTags = new ArrayList<>(entry.llmDeniedTags());

        if (hit.tag()) {
            moveCacheValue(hit.value(), moveToAllowed, allowedTags, deniedTags);
        } else {
            moveCacheValue(hit.value(), moveToAllowed, allowedItemIds, deniedItemIds);
        }

        this.bindings.set(hit.bindingIndex(), new CopperWrenchBindingsPayload.BindingEntry(
                entry.dimension(),
                entry.x(),
                entry.y(),
                entry.z(),
                entry.blockId(),
                entry.itemId(),
                entry.loaded(),
                entry.available(),
                entry.llmEnabled(),
                entry.llmPrompt(),
                entry.llmCachedItemIds(),
                entry.llmCachedTags(),
                allowedItemIds,
                deniedItemIds,
                allowedTags,
                deniedTags
        ));
        updateFields();

        if (ClientPlayNetworking.canSend(UpdateCopperGolemBindingCachePayload.TYPE)) {
            ClientPlayNetworking.send(new UpdateCopperGolemBindingCachePayload(
                    this.golemId,
                    entry.dimension(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    hit.value(),
                    hit.tag(),
                    moveToAllowed,
                    this.revision
            ));
        }
    }

    private void moveCacheValue(String value, boolean allowed, List<String> allowedValues, List<String> deniedValues) {
        if (allowed) {
            addIfMissing(allowedValues, value);
            deniedValues.remove(value);
        } else {
            addIfMissing(deniedValues, value);
            allowedValues.remove(value);
        }
    }

    private void addIfMissing(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
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
        ensureSelectedBindingVisible();
        updateFields();

        if (ClientPlayNetworking.canSend(UpdateCopperGolemBindingLlmPayload.TYPE)) {
            ClientPlayNetworking.send(new UpdateCopperGolemBindingLlmPayload(
                    this.golemId,
                    entry.dimension(),
                    entry.x(),
                    entry.y(),
                    entry.z(),
                    enabled,
                    prompt,
                    this.revision
            ));
        }
    }

    private void toggleGatheringLlm() {
        this.gatheringLlmEnabled = !this.gatheringLlmEnabled;
        if (this.gatheringLlmToggleButton != null) {
            this.gatheringLlmToggleButton.setMessage(gatheringLlmToggleText());
        }
        saveGatheringPrompt();
    }

    private void saveGatheringPrompt() {
        this.gatheringLlmPrompt = this.promptField != null && this.promptField.visible
                ? this.promptField.getValue().trim()
                : this.gatheringLlmPrompt == null ? "" : this.gatheringLlmPrompt;
        if (ClientPlayNetworking.canSend(UpdateCopperGolemGatheringLlmPayload.TYPE)) {
            ClientPlayNetworking.send(new UpdateCopperGolemGatheringLlmPayload(
                    this.golemId,
                    this.gatheringLlmEnabled,
                    this.gatheringLlmPrompt,
                    this.revision
            ));
        }
    }

    private void updateOperationButton() {
        if (this.operationButton != null) {
            this.operationButton.setMessage(operationButtonText());
        }
    }

    private void updateModeButton() {
        if (this.modeButton != null) {
            this.modeButton.setMessage(modeButtonText());
        }
    }

    private void updateWidgetLayout() {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
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
        if (this.modeButton != null) {
            this.modeButton.setX(panelX + panelWidth - PANEL_PADDING - 158);
            this.modeButton.setY(panelY + 7);
            this.modeButton.setWidth(78);
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
            this.promptField.setX(promptEditorX());
            this.promptField.setY(promptEditorY());
            this.promptField.setWidth(promptFieldWidth());
            this.promptField.visible = promptEditorVisible();
            this.promptField.active = this.promptField.visible;
            this.promptField.setEditable(this.promptField.active);
        }
        if (this.savePromptButton != null) {
            this.savePromptButton.setX(promptSaveButtonX());
            this.savePromptButton.setY(promptSaveButtonY());
            this.savePromptButton.setWidth(promptSaveButtonWidth());
            this.savePromptButton.visible = promptEditorVisible();
            this.savePromptButton.active = this.savePromptButton.visible;
        }
        if (this.gatheringLlmToggleButton != null) {
            this.gatheringLlmToggleButton.setX(gatheringLlmToggleX());
            this.gatheringLlmToggleButton.setY(gatheringLlmToggleY());
            this.gatheringLlmToggleButton.setWidth(62);
            this.gatheringLlmToggleButton.visible = this.activeTab == Tab.BINDINGS && isGatheringMode();
            this.gatheringLlmToggleButton.active = this.gatheringLlmToggleButton.visible;
            this.gatheringLlmToggleButton.setMessage(gatheringLlmToggleText());
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
        updateModeButton();

        boolean bindingsVisible = this.activeTab == Tab.BINDINGS;
        boolean promptVisible = promptEditorVisible();
        boolean llmVisible = this.activeTab == Tab.LLM;

        if (this.promptField != null) {
            this.promptField.visible = promptVisible;
            this.promptField.active = promptVisible;
            this.promptField.setEditable(this.promptField.active);
            String currentPromptContext = promptVisible ? promptEditorContext() : "";
            if (!currentPromptContext.equals(this.promptFieldContext)) {
                this.promptField.setValue(selectedBindingPrompt());
                this.promptFieldContext = currentPromptContext;
            }
        }
        if (this.savePromptButton != null) {
            this.savePromptButton.visible = promptVisible;
            this.savePromptButton.active = promptVisible;
            this.savePromptButton.setMessage(Component.literal("儲存"));
        }
        if (this.gatheringLlmToggleButton != null) {
            this.gatheringLlmToggleButton.visible = bindingsVisible && isGatheringMode();
            this.gatheringLlmToggleButton.active = this.gatheringLlmToggleButton.visible;
            this.gatheringLlmToggleButton.setMessage(gatheringLlmToggleText());
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
        if (isGatheringMode()) {
            return this.gatheringLlmPrompt == null ? "" : this.gatheringLlmPrompt;
        }
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return "";
        }
        return this.bindings.get(this.selectedBindingIndex).llmPrompt();
    }

    private String promptEditorContext() {
        if (this.activeTab != Tab.BINDINGS) {
            return "hidden";
        }
        if (isGatheringMode()) {
            return "gathering";
        }
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return "none";
        }

        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(this.selectedBindingIndex);
        return "sorting:" + entry.dimension() + ":" + entry.x() + ":" + entry.y() + ":" + entry.z();
    }

    private void drawSortingTab(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int panelX = panelX();
        int listX = panelX + PANEL_PADDING;
        int listY = sortingListY();
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int listHeight = getListHeight();

        extractor.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80101010);
        extractor.outline(listX, listY, listWidth, listHeight, 0xFF3A3A3A);

        if (this.bindings.isEmpty()) {
            extractor.centeredText(this.font, Component.translatable("message.deadrecall.copper_wrench.binding_list_empty"),
                    panelX + panelWidth() / 2, listY + listHeight / 2 - 4, 0xFFB8B8B8);
        } else {
            extractor.enableScissor(listX + 1, listY + 1, listX + listWidth - 1, listY + listHeight - 1);
            for (int i = 0; i < this.bindings.size(); i++) {
                int cardY = listY + bindingCardRelativeTop(i) - this.scrollOffset;
                int cardHeight = bindingCardHeight(i);
                if (cardY + cardHeight < listY || cardY > listY + listHeight) {
                    continue;
                }
                drawBindingCard(extractor, this.bindings.get(i), i, listX + 8, cardY, listWidth - 16, mouseX, mouseY);
            }
            extractor.disableScissor();
        }

        if (getMaxScroll() > 0) {
            drawScrollBar(extractor, listX + listWidth - 6, listY + 4, listHeight - 8);
        }
    }

    private void drawGatheringTab(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = panelX() + PANEL_PADDING;
        int width = panelWidth() - PANEL_PADDING * 2;
        int contentY = gatheringContentY();
        int height = Math.max(24, panelY() + panelHeight() - FOOTER_HEIGHT - 4 - contentY);

        extractor.fill(x, contentY, x + width, contentY + height, 0x80101010);
        extractor.outline(x, contentY, width, height, 0xFF3A3A3A);
        extractor.text(this.font, Component.translatable("message.deadrecall.copper_wrench.gathering_area"), x + 10, contentY + 8, 0xFFFFFFFF);
        extractor.text(this.font, gatheringCornerText(true), x + 10, contentY + 24, gatheringCornerColor(true));
        extractor.text(this.font, gatheringCornerText(false), x + 10, contentY + 38, gatheringCornerColor(false));
        extractor.text(this.font, gatheringAreaRangeText(), x + 10, contentY + 54, hasCompleteGatheringArea() ? 0xFF64D26D : 0xFFFFC857);
        drawGatheringInventorySlots(extractor, x + width - 142, contentY + 8, mouseX, mouseY);
        extractor.text(this.font, Component.translatable("message.deadrecall.copper_wrench.gathering_targets"), x + 10, contentY + 78, 0xFFFFFFFF);
        drawGatheringManualTargets(extractor, x + 10, contentY + 94, width - 20, height - 100, mouseX, mouseY);
    }

    private void drawGatheringInventorySlots(GuiGraphicsExtractor extractor, int x, int y, int mouseX, int mouseY) {
        drawGatheringSlot(extractor, x, y, Component.translatable("message.deadrecall.copper_wrench.gathering_tool_slot"),
                this.gatheringToolItemId, this.gatheringToolCount, this.gatheringToolDamage, this.gatheringToolMaxDamage,
                isGatheringToolSlotAt(mouseX, mouseY), gatheringToolTooltip(), mouseX, mouseY);
        drawGatheringSlot(extractor, x + 66, y, Component.translatable("message.deadrecall.copper_wrench.gathering_storage_slot"),
                this.gatheringStorageItemId, this.gatheringStorageCount, 0, 0,
                isGatheringStorageSlotAt(mouseX, mouseY), gatheringStorageTooltip(), mouseX, mouseY);
    }

    private void drawGatheringSlot(
            GuiGraphicsExtractor extractor,
            int x,
            int y,
            Component label,
            String itemId,
            int count,
            int damage,
            int maxDamage,
            boolean hovered,
            List<Component> tooltip,
            int mouseX,
            int mouseY) {
        extractor.text(this.font, label, x, y, 0xFFE0E0E0);
        extractor.fill(x, y + 12, x + 22, y + 34, 0xB0000000);
        extractor.outline(x, y + 12, 22, 22, count > 0 ? 0xFFE2C15A : 0xFF777777);
        if (count > 0) {
            extractor.item(iconStack(itemId), x + 3, y + 15);
            if (count > 1) {
                extractor.text(this.font, String.valueOf(count), x + 13, y + 26, 0xFFFFFFFF);
            }
            if (maxDamage > 0) {
                int remaining = Math.max(0, maxDamage - damage);
                int barWidth = Math.max(1, Math.min(20, remaining * 20 / maxDamage));
                extractor.fill(x + 1, y + 33, x + 1 + barWidth, y + 34, 0xFF64D26D);
            }
        } else {
            extractor.item(new ItemStack(Items.BARRIER), x + 3, y + 15);
            extractor.fill(x + 3, y + 15, x + 19, y + 31, 0x90000000);
        }
        if (hovered) {
            extractor.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void drawGatheringManualTargets(GuiGraphicsExtractor extractor, int x, int y, int width, int height, int mouseX, int mouseY) {
        List<GatheringTargetEntry> accepted = gatheringAcceptedTargetEntries();
        List<GatheringTargetEntry> denied = gatheringDeniedTargetEntries();
        extractor.text(this.font, Component.translatable("message.deadrecall.copper_wrench.gathering_llm_prompt"),
                x, y, 0xFFFFFFFF);
        extractor.text(this.font, Component.literal("快取方塊: " + this.gatheringLlmCachedBlockIds + " / 快取 Tag: " + this.gatheringLlmCachedTags),
                x + 70, y + 5, 0xFFB8B8B8);
        extractor.text(this.font,
                Component.translatable("message.deadrecall.copper_wrench.gathering_targets_count", accepted.size() + denied.size()),
                x, gatheringTargetHeaderY(), 0xFFB8B8B8);
        extractor.text(this.font, Component.literal("右鍵圖示移除目標"), x + 100, gatheringTargetHeaderY(), 0xFF909090);

        if (accepted.isEmpty() && denied.isEmpty()) {
            extractor.text(this.font, Component.translatable("message.deadrecall.copper_wrench.gathering_targets_empty"), x, gatheringTargetRowsStartY(), 0xFFFFC857);
            return;
        }

        int startY = gatheringTargetRowsStartY();
        int gap = 8;
        int groupWidth = (width - gap) / 2;
        drawGatheringTargetGroup(extractor, accepted, x, startY, groupWidth, 0xFF4C8A53, "接受目標", mouseX, mouseY);
        drawGatheringTargetGroup(extractor, denied, x + groupWidth + gap, startY, groupWidth, 0xFF9A4D4D, "拒絕", mouseX, mouseY);
    }

    private List<GatheringTargetEntry> gatheringAcceptedTargetEntries() {
        List<GatheringTargetEntry> entries = new ArrayList<>();
        Set<String> manualTargetSet = new LinkedHashSet<>();
        for (String blockId : this.gatheringManualTargets) {
            if (blockId == null || blockId.isBlank() || !manualTargetSet.add(blockId)) {
                continue;
            }
            entries.add(new GatheringTargetEntry(blockId, false, CopperGolemGatheringTargetPayload.TargetSet.MANUAL));
        }
        for (String blockId : this.gatheringLlmAllowedBlockIds) {
            if (blockId == null || blockId.isBlank() || manualTargetSet.contains(blockId)) {
                continue;
            }
            entries.add(new GatheringTargetEntry(blockId, false, CopperGolemGatheringTargetPayload.TargetSet.ALLOWED));
        }
        for (String tagId : this.gatheringLlmAllowedTags) {
            if (tagId != null && !tagId.isBlank()) {
                entries.add(new GatheringTargetEntry(tagId, true, CopperGolemGatheringTargetPayload.TargetSet.ALLOWED));
            }
        }
        return entries;
    }

    private List<GatheringTargetEntry> gatheringDeniedTargetEntries() {
        List<GatheringTargetEntry> entries = new ArrayList<>();
        for (String blockId : this.gatheringLlmDeniedBlockIds) {
            if (blockId != null && !blockId.isBlank()) {
                entries.add(new GatheringTargetEntry(blockId, false, CopperGolemGatheringTargetPayload.TargetSet.DENIED));
            }
        }
        for (String tagId : this.gatheringLlmDeniedTags) {
            if (tagId != null && !tagId.isBlank()) {
                entries.add(new GatheringTargetEntry(tagId, true, CopperGolemGatheringTargetPayload.TargetSet.DENIED));
            }
        }
        return entries;
    }

    private void drawGatheringTargetGroup(
            GuiGraphicsExtractor extractor,
            List<GatheringTargetEntry> entries,
            int x,
            int y,
            int width,
            int color,
            String label,
            int mouseX,
            int mouseY) {
        extractor.outline(x, y, width, 18, color);
        extractor.text(this.font, label, x + 4, y + 5, color);

        int iconX = gatheringTargetGroupIconX(x, width, label);
        int maxIcons = gatheringTargetGroupMaxIcons(x, width, label);
        int shown = Math.min(entries.size(), maxIcons);
        for (int i = 0; i < shown; i++) {
            GatheringTargetEntry entry = entries.get(i);
            int slotX = iconX + i * 18;
            extractor.fill(slotX, y + 1, slotX + 17, y + 18, 0xA0000000);
            extractor.outline(slotX, y + 1, 17, 17, color);
            extractor.item(entry.tag() ? new ItemStack(Items.NAME_TAG) : iconStack(entry.value()), slotX + 1, y + 2);
            if (mouseX >= slotX && mouseX <= slotX + 17 && mouseY >= y + 1 && mouseY <= y + 18) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal(label + (entry.tag() ? " Tag" : "方塊")),
                        Component.literal(entry.value()),
                        Component.literal("右鍵移除")
                ), mouseX, mouseY);
            }
        }

        int remaining = entries.size() - shown;
        if (remaining > 0) {
            extractor.text(this.font, "+" + remaining, iconX + shown * 18 + 2, y + 5, 0xFFE0E0E0);
        }
    }

    private int gatheringTargetGroupIconX(int x, int width, String label) {
        int offset = Math.max(46, this.font.width(label) + 8);
        return Math.min(x + Math.max(0, width - 18), x + offset);
    }

    private int gatheringTargetGroupMaxIcons(int x, int width, String label) {
        int iconX = gatheringTargetGroupIconX(x, width, label);
        return Math.max(0, (x + width - iconX - 4) / 18);
    }

    private List<GatheringPreviewRow> gatheringPreviewRows() {
        List<GatheringPreviewRow> rows = new ArrayList<>();
        for (int i = 0; i < this.gatheringManualTargets.size(); i++) {
            String blockId = this.gatheringManualTargets.get(i);
            rows.add(new GatheringPreviewRow(
                    blockDisplayName(blockId).getString(),
                    blockId,
                    blockId,
                    true,
                    i,
                    0xFFE0E0E0
            ));
        }
        addGatheringCacheRows(rows, this.gatheringLlmAllowedBlockIds, "Prompt 允許", true, 0xFF64D26D);
        addGatheringCacheRows(rows, this.gatheringLlmDeniedBlockIds, "Prompt 拒絕", true, 0xFFFF8A8A);
        addGatheringCacheRows(rows, this.gatheringLlmAllowedTags, "Prompt 允許 #", false, 0xFF64D26D);
        addGatheringCacheRows(rows, this.gatheringLlmDeniedTags, "Prompt 拒絕 #", false, 0xFFFF8A8A);
        return rows;
    }

    private void addGatheringCacheRows(List<GatheringPreviewRow> rows, List<String> values, String prefix, boolean blockId, int color) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String label = blockId ? blockDisplayName(value).getString() : prefix + value;
            rows.add(new GatheringPreviewRow(
                    label,
                    value,
                    blockId ? value : "minecraft:name_tag",
                    false,
                    -1,
                    color
            ));
        }
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
        int cardHeight = bindingCardHeight(index);
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + cardHeight;
        boolean selected = index == this.selectedBindingIndex;
        int borderColor = selected ? 0xFFE2C15A : entry.available() ? 0xFF4C8A53 : entry.loaded() ? 0xFF9A4D4D : 0xFF777777;
        extractor.fill(x, y, x + width, y + cardHeight, hovered || selected ? 0xC02A2A2A : 0xB0222222);
        extractor.outline(x, y, width, cardHeight, borderColor);

        extractor.fill(x + 8, y + 7, x + 28, y + 27, 0xB0000000);
        extractor.item(iconStack(entry.itemId()), x + 10, y + 9);

        String title = "目標容器 #" + (index + 1);
        extractor.text(this.font, trimToWidth(title, width - 48), x + 38, selected ? y + 5 : y + 9, 0xFFFFFFFF);
        extractor.text(this.font, trimToWidth(blockDisplayName(entry.blockId()).getString(), width - 48), x + 38, selected ? y + 18 : y + 25, 0xFFB8B8B8);

        if (selected) {
            if (entry.llmEnabled()) {
                extractor.text(this.font, "Prompt", x + 38, y + 32, 0xFFE0E0E0);
            }

            int buttonX = x + width - 62;
            int buttonY = y + 28;
            extractor.fill(buttonX, buttonY, buttonX + 52, buttonY + 16, entry.llmEnabled() ? 0xFF326A3D : 0xFF4A4A4A);
            extractor.outline(buttonX, buttonY, 52, 16, entry.llmEnabled() ? 0xFF74D17B : 0xFF7A7A7A);
            extractor.centeredText(this.font, Component.literal(entry.llmEnabled() ? "LLM 開" : "LLM 關"), buttonX + 26, buttonY + 4, 0xFFFFFFFF);
            drawBindingCachePreview(extractor, entry, x + 38, bindingCachePreviewY(y, selected), width - 48, mouseX, mouseY);
        }

        if (hovered) {
            extractor.setComponentTooltipForNextFrame(this.font, bindingTooltip(entry), mouseX, mouseY);
        }
    }

    private Component operationButtonText() {
        return Component.translatable(this.running
                ? "message.deadrecall.copper_wrench.action_stop"
                : "message.deadrecall.copper_wrench.action_start");
    }

    private Component modeButtonText() {
        return Component.translatable(modeTranslationKey());
    }

    private Component gatheringLlmToggleText() {
        return Component.literal(this.gatheringLlmEnabled ? "LLM 開" : "LLM 關");
    }

    private Component operationStatusText() {
        if (!this.running) {
            return Component.translatable("message.deadrecall.copper_wrench.operation_stopped");
        }
        String normalizedActivity = normalizedActivity();
        if (!normalizedActivity.isBlank()) {
            return Component.translatable("message.deadrecall.copper_wrench.activity_" + normalizedActivity);
        }
        return Component.translatable("message.deadrecall.copper_wrench.operation_running");
    }

    private int operationStatusColor() {
        if (this.running && (normalizedActivity().startsWith("blocked_") || !hasFuelAvailable())) {
            return 0xFFFFC857;
        }
        return this.running ? 0xFF64D26D : 0xFFFF6B6B;
    }

    private String normalizedMode() {
        return "gathering".equals(this.mode) ? "gathering" : "sorting";
    }

    private String normalizedActivity() {
        return this.activity == null ? "" : this.activity;
    }

    private String modeTranslationKey() {
        return "message.deadrecall.copper_wrench.mode_" + normalizedMode();
    }

    private boolean hasFuelAvailable() {
        return this.fuelTicks > 0 || hasFuelItem();
    }

    private boolean hasFuelItem() {
        return this.fuelCount > 0
                && this.minecraft != null
                && this.minecraft.level != null
                && this.minecraft.level.fuelValues().isFuel(iconStack(this.fuelItemId));
    }

    private void drawSourceIcon(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = sourceIconX();
        int y = sourceIconY();

        extractor.fill(x, y, x + 20, y + 20, 0xB0000000);
        extractor.outline(x, y, 20, 20, sourceAvailable() ? 0xFFB97836 : 0xFF777777);
        if (this.sourceContainer == null) {
            extractor.item(new ItemStack(Items.CHEST), x + 2, y + 2);
            extractor.fill(x + 2, y + 2, x + 18, y + 18, 0x90000000);
        } else {
            extractor.item(iconStack(this.sourceContainer.itemId()), x + 2, y + 2);
        }

        if (isSourceIconAt(mouseX, mouseY)) {
            extractor.setComponentTooltipForNextFrame(this.font, sourceTooltip(), mouseX, mouseY);
        }
    }

    private void drawFuelSlot(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int slotX = fuelSlotX();
        int slotY = fuelSlotY();
        int labelX = sourceIconX() - 74;

        if (labelX > panelX() + PANEL_PADDING + 150) {
            extractor.text(this.font, Component.translatable("message.deadrecall.copper_wrench.fuel_slot"), labelX, slotY + 2, 0xFFE0E0E0);
            extractor.text(this.font, fuelTicksText(), labelX, slotY + 13, 0xFFB8B8B8);
        }

        extractor.fill(slotX, slotY, slotX + 20, slotY + 20, 0xB0000000);
        extractor.outline(slotX, slotY, 20, 20, hasFuelAvailable() ? 0xFFE2C15A : 0xFF777777);
        if (this.fuelCount > 0) {
            extractor.item(iconStack(this.fuelItemId), slotX + 2, slotY + 2);
            extractor.text(this.font, String.valueOf(this.fuelCount), slotX + 11, slotY + 12, 0xFFFFFFFF);
        } else {
            extractor.item(new ItemStack(Items.COAL), slotX + 2, slotY + 2);
            extractor.fill(slotX + 2, slotY + 2, slotX + 18, slotY + 18, 0x90000000);
        }

        if (this.fuelTicks > 0) {
            int barWidth = Math.max(1, Math.min(18, this.fuelTicks * 18 / 1600));
            extractor.fill(slotX + 1, slotY + 18, slotX + 1 + barWidth, slotY + 19, 0xFFFFB238);
        }

        if (isFuelSlotAt(mouseX, mouseY)) {
            extractor.setComponentTooltipForNextFrame(this.font, fuelTooltip(), mouseX, mouseY);
        }
    }

    private String fuelTicksText() {
        return this.fuelTicks > 0 ? this.fuelTicks + " tick" : "-";
    }

    private List<Component> fuelTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.fuel_slot"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.fuel_left_click"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.fuel_right_click"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.fuel_ticks", this.fuelTicks));
        if (this.fuelCount > 0) {
            tooltip.add(Component.literal(this.fuelItemId + " x" + this.fuelCount));
        }
        return tooltip;
    }

    private List<Component> gatheringToolTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_tool_slot"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_tool_left_click"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_tool_right_click"));
        if (this.gatheringToolCount > 0) {
            tooltip.add(itemDisplayName(this.gatheringToolItemId));
            if (this.gatheringToolMaxDamage > 0) {
                tooltip.add(Component.translatable(
                        "message.deadrecall.copper_wrench.gathering_tool_durability",
                        Math.max(0, this.gatheringToolMaxDamage - this.gatheringToolDamage),
                        this.gatheringToolMaxDamage));
            }
        }
        return tooltip;
    }

    private List<Component> gatheringStorageTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_storage_slot"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_storage_right_click"));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.gathering_storage_count", this.gatheringStorageCount, 16));
        if (this.gatheringStorageCount > 0) {
            tooltip.add(itemDisplayName(this.gatheringStorageItemId));
        }
        return tooltip;
    }

    private String selectedBindingLabel() {
        if (this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return "Prompt";
        }
        return "箱子 #" + (this.selectedBindingIndex + 1);
    }

    private void drawBindingCachePreview(GuiGraphicsExtractor extractor, CopperWrenchBindingsPayload.BindingEntry entry, int x, int y, int width, int mouseX, int mouseY) {
        int gap = 8;
        int groupWidth = (width - gap) / 2;
        drawCacheItemGroup(extractor, cachePreviewEntries(entry.llmAllowedItemIds(), entry.llmAllowedTags()),
                x, y, groupWidth, 0xFF4C8A53, "接受", mouseX, mouseY);
        drawCacheItemGroup(extractor, cachePreviewEntries(entry.llmDeniedItemIds(), entry.llmDeniedTags()),
                x + groupWidth + gap, y, groupWidth, 0xFF9A4D4D, "拒絕", mouseX, mouseY);
    }

    private List<CachePreviewEntry> cachePreviewEntries(List<String> itemIds, List<String> tagIds) {
        List<CachePreviewEntry> entries = new ArrayList<>(itemIds.size() + tagIds.size());
        for (String itemId : itemIds) {
            entries.add(new CachePreviewEntry(itemId, false));
        }
        for (String tagId : tagIds) {
            entries.add(new CachePreviewEntry(tagId, true));
        }
        return entries;
    }

    private CachePreviewHit cachePreviewHitAt(int bindingIndex, double mouseX, double mouseY) {
        if (bindingIndex < 0 || bindingIndex >= this.bindings.size()) {
            return null;
        }
        if (bindingIndex != this.selectedBindingIndex) {
            return null;
        }

        int listX = panelX() + PANEL_PADDING;
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int cardX = listX + 8;
        int cardY = bindingCardY(bindingIndex);
        int cardWidth = listWidth - 16;
        int cacheX = cardX + 38;
        int cacheY = bindingCachePreviewY(cardY, true);
        int cacheWidth = cardWidth - 48;
        int gap = 8;
        int groupWidth = (cacheWidth - gap) / 2;

        CopperWrenchBindingsPayload.BindingEntry entry = this.bindings.get(bindingIndex);
        CachePreviewHit allowedHit = cachePreviewGroupHitAt(
                bindingIndex,
                cachePreviewEntries(entry.llmAllowedItemIds(), entry.llmAllowedTags()),
                true,
                cacheX,
                cacheY,
                groupWidth,
                mouseX,
                mouseY);
        if (allowedHit != null) {
            return allowedHit;
        }

        return cachePreviewGroupHitAt(
                bindingIndex,
                cachePreviewEntries(entry.llmDeniedItemIds(), entry.llmDeniedTags()),
                false,
                cacheX + groupWidth + gap,
                cacheY,
                groupWidth,
                mouseX,
                mouseY);
    }

    private CachePreviewHit cachePreviewGroupHitAt(
            int bindingIndex,
            List<CachePreviewEntry> entries,
            boolean acceptedSide,
            int x,
            int y,
            int width,
            double mouseX,
            double mouseY) {
        int iconX = x + 28;
        int maxIcons = Math.max(0, (width - 34) / 18);
        int shown = Math.min(entries.size(), maxIcons);
        if (shown <= 0 || mouseY < y + 1 || mouseY > y + 18 || mouseX < iconX) {
            return null;
        }

        int index = ((int) mouseX - iconX) / 18;
        if (index < 0 || index >= shown) {
            return null;
        }

        int slotX = iconX + index * 18;
        if (mouseX > slotX + 17) {
            return null;
        }

        CachePreviewEntry entry = entries.get(index);
        return new CachePreviewHit(bindingIndex, entry.value(), entry.tag(), acceptedSide);
    }

    private void drawCacheItemGroup(GuiGraphicsExtractor extractor, List<CachePreviewEntry> entries, int x, int y, int width, int color, String label, int mouseX, int mouseY) {
        extractor.outline(x, y, width, 18, color);
        extractor.text(this.font, label, x + 4, y + 5, color);

        int iconX = x + 28;
        int maxIcons = Math.max(0, (width - 34) / 18);
        int shown = Math.min(entries.size(), maxIcons);
        for (int i = 0; i < shown; i++) {
            CachePreviewEntry entry = entries.get(i);
            int slotX = iconX + i * 18;
            extractor.fill(slotX, y + 1, slotX + 17, y + 18, 0xA0000000);
            extractor.outline(slotX, y + 1, 17, 17, color);
            extractor.item(entry.tag() ? new ItemStack(Items.NAME_TAG) : iconStack(entry.value()), slotX + 1, y + 2);
            if (mouseX >= slotX && mouseX <= slotX + 17 && mouseY >= y + 1 && mouseY <= y + 18) {
                extractor.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.literal(label + (entry.tag() ? " Tag" : "物品")),
                        Component.literal(entry.value()),
                        Component.literal("右鍵移到" + ("接受".equals(label) ? "拒絕" : "接受"))
                ), mouseX, mouseY);
            }
        }

        int remaining = entries.size() - shown;
        if (remaining > 0) {
            extractor.text(this.font, "+" + remaining, iconX + shown * 18 + 2, y + 5, 0xFFE0E0E0);
        }
    }

    private List<Component> bindingTooltip(CopperWrenchBindingsPayload.BindingEntry entry) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(blockDisplayName(entry.blockId()));
        tooltip.add(Component.literal("ID: " + entry.blockId()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_position", entry.x(), entry.y(), entry.z()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_status", Component.literal(statusText(entry))));
        tooltip.add(Component.literal("LLM: " + (entry.llmEnabled() ? "啟用" : "停用")));
        tooltip.add(Component.literal("快取物品: " + entry.llmCachedItemIds() + " / 快取 Tag: " + entry.llmCachedTags()));
        tooltip.add(Component.literal("接受/拒絕圖示包含物品與 Tag；Tag 以命名牌顯示"));
        tooltip.add(Component.literal("右鍵快取圖示可移到另一邊"));
        return tooltip;
    }

    private boolean isLlmToggleAt(int index, double mouseX, double mouseY) {
        if (index != this.selectedBindingIndex) {
            return false;
        }
        int listX = panelX() + PANEL_PADDING;
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int cardX = listX + 8;
        int cardY = bindingCardY(index);
        int buttonX = cardX + listWidth - 16 - 62;
        int buttonY = cardY + 28;
        return mouseX >= buttonX && mouseX <= buttonX + 52 && mouseY >= buttonY && mouseY <= buttonY + 16;
    }

    private int bindingCachePreviewY(int cardY, boolean selected) {
        return selected ? cardY + 74 : cardY + 48;
    }

    private int bindingIndexAt(double mouseX, double mouseY) {
        int listX = panelX() + PANEL_PADDING;
        int listY = sortingListY();
        int listWidth = panelWidth() - PANEL_PADDING * 2;
        int listHeight = getListHeight();
        if (mouseX < listX + 1 || mouseX > listX + listWidth - 1 || mouseY < listY + 1 || mouseY > listY + listHeight - 1) {
            return -1;
        }

        int relativeY = (int) mouseY - listY + this.scrollOffset;
        if (relativeY < 0) {
            return -1;
        }

        for (int i = 0; i < this.bindings.size(); i++) {
            int cardTop = bindingCardRelativeTop(i);
            int cardBottom = cardTop + bindingCardHeight(i);
            if (relativeY >= cardTop && relativeY <= cardBottom) {
                return i;
            }
            if (relativeY < cardTop) {
                return -1;
            }
        }
        return -1;
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

    private Component itemDisplayName(String itemId) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null) {
            return Component.literal(itemId);
        }
        return BuiltInRegistries.ITEM.getOptional(identifier)
                .map(item -> item.getName(new ItemStack(item)))
                .orElse(Component.literal(itemId));
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
        return Math.max(24, panelY() + panelHeight() - FOOTER_HEIGHT - 4 - sortingListY());
    }

    private int getContentHeight() {
        int height = 7;
        for (int i = 0; i < this.bindings.size(); i++) {
            height += bindingCardHeight(i) + CARD_GAP;
        }
        return height + 3;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - getListHeight());
    }

    private boolean isFuelSlotAt(double mouseX, double mouseY) {
        int slotX = fuelSlotX();
        int slotY = fuelSlotY();
        return mouseX >= slotX && mouseX <= slotX + 20 && mouseY >= slotY && mouseY <= slotY + 20;
    }

    private boolean isSourceIconAt(double mouseX, double mouseY) {
        int slotX = sourceIconX();
        int slotY = sourceIconY();
        return mouseX >= slotX && mouseX <= slotX + 20 && mouseY >= slotY && mouseY <= slotY + 20;
    }

    private boolean isGatheringToolSlotAt(double mouseX, double mouseY) {
        int slotX = gatheringToolSlotX();
        int slotY = gatheringSlotY();
        return mouseX >= slotX && mouseX <= slotX + 22 && mouseY >= slotY && mouseY <= slotY + 22;
    }

    private boolean isGatheringStorageSlotAt(double mouseX, double mouseY) {
        int slotX = gatheringStorageSlotX();
        int slotY = gatheringSlotY();
        return mouseX >= slotX && mouseX <= slotX + 22 && mouseY >= slotY && mouseY <= slotY + 22;
    }

    private boolean isPromptWidgetAt(double mouseX, double mouseY) {
        return isWidgetAt(this.promptField, mouseX, mouseY)
                || isWidgetAt(this.savePromptButton, mouseX, mouseY)
                || isWidgetAt(this.gatheringLlmToggleButton, mouseX, mouseY);
    }

    private boolean isWidgetAt(AbstractWidget widget, double mouseX, double mouseY) {
        return widget != null
                && widget.visible
                && mouseX >= widget.getX()
                && mouseX <= widget.getX() + widget.getWidth()
                && mouseY >= widget.getY()
                && mouseY <= widget.getY() + widget.getHeight();
    }

    private GatheringTargetHit gatheringTargetHitAt(double mouseX, double mouseY) {
        int x = gatheringTargetsX();
        int y = gatheringTargetRowsStartY();
        int width = gatheringTargetsWidth();
        int gap = 8;
        int groupWidth = (width - gap) / 2;

        GatheringTargetHit acceptedHit = gatheringTargetGroupHitAt(
                gatheringAcceptedTargetEntries(),
                x,
                y,
                groupWidth,
                "接受目標",
                mouseX,
                mouseY);
        if (acceptedHit != null) {
            return acceptedHit;
        }

        return gatheringTargetGroupHitAt(
                gatheringDeniedTargetEntries(),
                x + groupWidth + gap,
                y,
                groupWidth,
                "拒絕",
                mouseX,
                mouseY);
    }

    private GatheringTargetHit gatheringTargetGroupHitAt(
            List<GatheringTargetEntry> entries,
            int x,
            int y,
            int width,
            String label,
            double mouseX,
            double mouseY) {
        int iconX = gatheringTargetGroupIconX(x, width, label);
        int shown = Math.min(entries.size(), gatheringTargetGroupMaxIcons(x, width, label));
        if (shown <= 0 || mouseY < y + 1 || mouseY > y + 18 || mouseX < iconX) {
            return null;
        }

        int index = ((int) mouseX - iconX) / 18;
        if (index < 0 || index >= shown) {
            return null;
        }

        int slotX = iconX + index * 18;
        if (mouseX > slotX + 17) {
            return null;
        }

        GatheringTargetEntry entry = entries.get(index);
        return new GatheringTargetHit(entry.value(), entry.tag(), entry.targetSet());
    }

    private int gatheringTargetRemoveIndexAt(double mouseX, double mouseY) {
        int x = gatheringTargetsX();
        int y = gatheringTargetsY();
        int width = gatheringTargetsWidth();
        int height = gatheringTargetsHeight();
        List<GatheringPreviewRow> rows = gatheringPreviewRows();
        int rowHeight = 18;
        int startY = gatheringTargetRowsStartY();
        int maxRows = Math.max(0, (y + height - startY) / rowHeight);
        int shown = Math.min(rows.size(), maxRows);
        int removeX = x + width - 17;

        if (mouseX < removeX || mouseX > removeX + 15 || mouseY < startY || mouseY > startY + shown * rowHeight) {
            return -1;
        }

        int index = ((int) mouseY - startY) / rowHeight;
        int rowY = startY + index * rowHeight;
        if (index < 0 || index >= shown || mouseY > rowY + 17 || !rows.get(index).removable()) {
            return -1;
        }
        return rows.get(index).manualIndex();
    }

    private int fuelSlotX() {
        return panelX() + panelWidth() - PANEL_PADDING - 22;
    }

    private int fuelSlotY() {
        return panelY() + 26;
    }

    private int sourceIconX() {
        return Math.max(panelX() + PANEL_PADDING, fuelSlotX() - 24);
    }

    private int sourceIconY() {
        return fuelSlotY();
    }

    private int gatheringToolSlotX() {
        return panelX() + panelWidth() - PANEL_PADDING - 142;
    }

    private int gatheringStorageSlotX() {
        return gatheringToolSlotX() + 66;
    }

    private int gatheringSlotY() {
        return gatheringContentY() + 20;
    }

    private int gatheringTargetsX() {
        return panelX() + PANEL_PADDING + 10;
    }

    private int gatheringTargetsY() {
        return gatheringContentY() + 94;
    }

    private int gatheringTargetsWidth() {
        return panelWidth() - PANEL_PADDING * 2 - 20;
    }

    private int gatheringTargetsHeight() {
        int height = Math.max(24, panelY() + panelHeight() - FOOTER_HEIGHT - 4 - gatheringContentY());
        return height - 100;
    }

    private int gatheringTargetHeaderY() {
        return gatheringTargetsY() + 48;
    }

    private int gatheringTargetRowsStartY() {
        return gatheringTargetsY() + 64;
    }

    private int sortingListY() {
        return contentStartY();
    }

    private int gatheringContentY() {
        return contentStartY();
    }

    private int selectedBindingCardX() {
        return panelX() + PANEL_PADDING + 8;
    }

    private int selectedBindingCardY() {
        if (this.selectedBindingIndex < 0) {
            return sortingListY();
        }
        return bindingCardY(this.selectedBindingIndex);
    }

    private int selectedBindingCardWidth() {
        return panelWidth() - PANEL_PADDING * 2 - 16;
    }

    private int bindingCardHeight(int index) {
        return index == this.selectedBindingIndex ? TARGET_CARD_EXPANDED_HEIGHT : TARGET_CARD_COMPACT_HEIGHT;
    }

    private int bindingCardRelativeTop(int index) {
        int top = 7;
        for (int i = 0; i < index; i++) {
            top += bindingCardHeight(i) + CARD_GAP;
        }
        return top;
    }

    private int bindingCardY(int index) {
        return sortingListY() + bindingCardRelativeTop(index) - this.scrollOffset;
    }

    private boolean hasSelectedBinding() {
        return this.selectedBindingIndex >= 0 && this.selectedBindingIndex < this.bindings.size();
    }

    private boolean selectedBindingLlmEnabled() {
        return hasSelectedBinding() && this.bindings.get(this.selectedBindingIndex).llmEnabled();
    }

    private void ensureSelectedBindingVisible() {
        if (isGatheringMode() || this.selectedBindingIndex < 0 || this.selectedBindingIndex >= this.bindings.size()) {
            return;
        }

        int listHeight = getListHeight();
        int cardTop = bindingCardRelativeTop(this.selectedBindingIndex);
        int cardHeight = bindingCardHeight(this.selectedBindingIndex);
        int cardBottom = cardTop + cardHeight;
        if (listHeight <= cardHeight + 2) {
            this.scrollOffset = Math.min(Math.max(0, cardTop - 1), getMaxScroll());
            return;
        }

        int visibleTop = this.scrollOffset + 1;
        int visibleBottom = this.scrollOffset + listHeight - 1;

        if (cardTop < visibleTop) {
            this.scrollOffset = Math.max(0, cardTop - 1);
        } else if (cardBottom > visibleBottom) {
            this.scrollOffset = Math.max(0, cardBottom - listHeight + 1);
        }
        this.scrollOffset = Math.min(this.scrollOffset, getMaxScroll());
    }

    private boolean promptEditorVisible() {
        if (this.activeTab != Tab.BINDINGS) {
            return false;
        }
        return isGatheringMode() ? this.gatheringLlmEnabled : selectedBindingLlmEnabled();
    }

    private int promptEditorX() {
        if (isGatheringMode()) {
            return gatheringTargetsX() + 70;
        }
        if (hasSelectedBinding()) {
            return selectedBindingCardX() + 38;
        }
        return panelX() + PANEL_PADDING + 8;
    }

    private int promptEditorY() {
        if (isGatheringMode()) {
            return gatheringTargetsY() + 22;
        }
        if (hasSelectedBinding()) {
            return selectedBindingCardY() + 46;
        }
        return contentStartY() + 31;
    }

    private int promptSaveButtonX() {
        return promptEditorX() + promptFieldWidth() + 8;
    }

    private int promptSaveButtonY() {
        return promptEditorY();
    }

    private int gatheringLlmToggleX() {
        return gatheringTargetsX();
    }

    private int gatheringLlmToggleY() {
        return gatheringTargetsY() + 22;
    }

    private int panelX() {
        return Math.max(0, (this.width - panelWidth()) / 2);
    }

    private int panelY() {
        return Math.max(0, (this.height - panelHeight()) / 2);
    }

    private int contentStartY() {
        return panelY() + HEADER_HEIGHT + 6;
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
        if (isGatheringMode()) {
            return Math.max(80, gatheringTargetsWidth() - 70 - promptSaveButtonWidth() - 8);
        }
        if (hasSelectedBinding()) {
            return Math.max(80, selectedBindingCardWidth() - 38 - promptSaveButtonWidth() - 18);
        }
        return Math.max(80, panelWidth() - PANEL_PADDING * 2 - promptSaveButtonWidth() - 24);
    }

    private int promptSaveButtonWidth() {
        return 54;
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

    private boolean isGatheringMode() {
        return "gathering".equals(normalizedMode());
    }

    private boolean sourceAvailable() {
        return this.sourceContainer != null && this.sourceContainer.loaded() && this.sourceContainer.available();
    }

    private String sourceStatusText() {
        if (this.sourceContainer == null) {
            return Component.translatable("message.deadrecall.copper_wrench.source_unbound").getString();
        }
        return statusText(this.sourceContainer);
    }

    private List<Component> sourceTooltip() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.source_container"));
        if (this.sourceContainer == null) {
            tooltip.add(Component.translatable("message.deadrecall.copper_wrench.source_unbound"));
            return tooltip;
        }

        tooltip.add(blockDisplayName(this.sourceContainer.blockId()));
        tooltip.add(Component.literal("ID: " + this.sourceContainer.blockId()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_position", this.sourceContainer.x(), this.sourceContainer.y(), this.sourceContainer.z()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.binding_status", Component.literal(sourceStatusText())));
        return tooltip;
    }

    private Component gatheringCornerText(boolean cornerA) {
        if (this.gatheringArea == null || (cornerA && !this.gatheringArea.hasCornerA()) || (!cornerA && !this.gatheringArea.hasCornerB())) {
            return Component.translatable(cornerA
                    ? "message.deadrecall.copper_wrench.gathering_corner_a"
                    : "message.deadrecall.copper_wrench.gathering_corner_b",
                    Component.translatable("message.deadrecall.copper_wrench.gathering_corner_missing"));
        }

        int x = cornerA ? this.gatheringArea.cornerAX() : this.gatheringArea.cornerBX();
        int y = cornerA ? this.gatheringArea.cornerAY() : this.gatheringArea.cornerBY();
        int z = cornerA ? this.gatheringArea.cornerAZ() : this.gatheringArea.cornerBZ();
        return Component.translatable(cornerA
                ? "message.deadrecall.copper_wrench.gathering_corner_a"
                : "message.deadrecall.copper_wrench.gathering_corner_b",
                x + ", " + y + ", " + z);
    }

    private int gatheringCornerColor(boolean cornerA) {
        boolean present = this.gatheringArea != null && (cornerA ? this.gatheringArea.hasCornerA() : this.gatheringArea.hasCornerB());
        return present ? 0xFFE0E0E0 : 0xFFFFC857;
    }

    private Component gatheringAreaRangeText() {
        if (!hasCompleteGatheringArea()) {
            return Component.translatable("message.deadrecall.copper_wrench.gathering_area_incomplete");
        }

        int minX = Math.min(this.gatheringArea.cornerAX(), this.gatheringArea.cornerBX());
        int minY = Math.min(this.gatheringArea.cornerAY(), this.gatheringArea.cornerBY());
        int minZ = Math.min(this.gatheringArea.cornerAZ(), this.gatheringArea.cornerBZ());
        int maxX = Math.max(this.gatheringArea.cornerAX(), this.gatheringArea.cornerBX());
        int maxY = Math.max(this.gatheringArea.cornerAY(), this.gatheringArea.cornerBY());
        int maxZ = Math.max(this.gatheringArea.cornerAZ(), this.gatheringArea.cornerBZ());
        return Component.translatable("message.deadrecall.copper_wrench.gathering_area_range",
                minX + ", " + minY + ", " + minZ,
                maxX + ", " + maxY + ", " + maxZ);
    }

    private boolean hasCompleteGatheringArea() {
        return this.gatheringArea != null && this.gatheringArea.hasCornerA() && this.gatheringArea.hasCornerB();
    }

    private record GatheringPreviewRow(String label, String value, String iconItemId, boolean removable, int manualIndex, int color) {
    }

    private record GatheringTargetEntry(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet targetSet) {
    }

    private record GatheringTargetHit(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet targetSet) {
    }

    private record CachePreviewEntry(String value, boolean tag) {
    }

    private record CachePreviewHit(int bindingIndex, String value, boolean tag, boolean acceptedSide) {
    }

    private enum Tab {
        BINDINGS,
        LLM
    }
}
