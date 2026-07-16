package com.adaptor.deadrecall.client;

import com.adaptor.deadrecall.network.DeathNodeAdminPayload;
import com.adaptor.deadrecall.network.ManageDeathNodeAdminPayload;
import com.adaptor.deadrecall.network.RequestDeathNodeAdminPayload;
import com.adaptor.deadrecall.space.DeathNodeAdminService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DeathNodeAdminScreen extends Screen {
    public static DeathNodeAdminScreen CURRENT;

    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int HEADER_HEIGHT = 34;
    private static final int CONTROL_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_HEIGHT = 34;

    private DeathNodeAdminPayload payload;
    private UUID ownerFilter;
    private UUID selectedNodeId;
    private UUID pendingPurgeId;
    private StatusFilter statusFilter = StatusFilter.ALL;
    private int scrollIndex;

    private Button previousOwnerButton;
    private Button ownerFilterButton;
    private Button nextOwnerButton;
    private Button statusFilterButton;
    private Button refreshButton;
    private Button disableButton;
    private Button purgeButton;
    private Button doneButton;

    public DeathNodeAdminScreen(DeathNodeAdminPayload payload) {
        super(Component.literal("死亡節點管理"));
        this.payload = payload;
        this.selectedNodeId = payload.entries().stream().findFirst().map(DeathNodeAdminPayload.Entry::id).orElse(null);
        CURRENT = this;
    }

    @Override
    protected void init() {
        CURRENT = this;

        this.previousOwnerButton = Button.builder(Component.literal("<"), button -> cycleOwner(-1))
                .bounds(ownerPreviousX(), controlsY(), 20, 18)
                .build();
        this.addRenderableWidget(this.previousOwnerButton);

        this.ownerFilterButton = Button.builder(ownerFilterText(), button -> cycleOwner(1))
                .bounds(ownerFilterX(), controlsY(), ownerFilterWidth(), 18)
                .build();
        this.addRenderableWidget(this.ownerFilterButton);

        this.nextOwnerButton = Button.builder(Component.literal(">"), button -> cycleOwner(1))
                .bounds(ownerNextX(), controlsY(), 20, 18)
                .build();
        this.addRenderableWidget(this.nextOwnerButton);

        this.statusFilterButton = Button.builder(statusFilterText(), button -> cycleStatus())
                .bounds(statusFilterX(), controlsY(), 96, 18)
                .build();
        this.addRenderableWidget(this.statusFilterButton);

        this.refreshButton = Button.builder(Component.literal("重新整理"), button -> requestRefresh())
                .bounds(refreshX(), footerY(), 72, 18)
                .build();
        this.addRenderableWidget(this.refreshButton);

        this.disableButton = Button.builder(Component.literal("停用節點"), button -> disableSelected())
                .bounds(disableX(), footerY(), 72, 18)
                .build();
        this.addRenderableWidget(this.disableButton);

        this.purgeButton = Button.builder(purgeButtonText(), button -> purgeSelected())
                .bounds(purgeX(), footerY(), 88, 18)
                .build();
        this.addRenderableWidget(this.purgeButton);

        this.doneButton = Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(doneX(), footerY(), 62, 18)
                .build();
        this.addRenderableWidget(this.doneButton);

        updateButtons();
    }

    @Override
    public void removed() {
        super.removed();
        if (CURRENT == this) {
            CURRENT = null;
        }
    }

    public void applyPayload(DeathNodeAdminPayload payload) {
        this.payload = payload;
        if (this.ownerFilter != null && ownerOptions().stream().noneMatch(owner -> owner.id().equals(this.ownerFilter))) {
            this.ownerFilter = null;
        }
        if (selectedEntry() == null) {
            this.selectedNodeId = filteredEntries().stream().findFirst().map(DeathNodeAdminPayload.Entry::id).orElse(null);
        }
        this.scrollIndex = Math.min(this.scrollIndex, maxScrollIndex());
        this.pendingPurgeId = null;
        updateButtons();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractor.fill(0, 0, this.width, this.height, 0xA0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();

        extractor.fill(x, y, x + width, y + height, 0xF016191D);
        extractor.outline(x, y, width, height, 0xFF657383);
        extractor.text(this.font, this.title, x + PANEL_PADDING, y + 10, 0xFFFFFFFF);
        extractor.text(this.font, countSummary(), x + width - PANEL_PADDING - 180, y + 10, 0xFFB8C0C8);

        drawEntries(extractor, mouseX, mouseY);
        if (this.payload.truncated()) {
            extractor.text(this.font, "節點數量超過傳輸上限，只顯示前 " + DeathNodeAdminPayload.MAX_ENTRIES + " 筆。",
                    x + PANEL_PADDING, y + height - 34, 0xFFFFC857);
        }
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        UUID hit = entryAt(event.x(), event.y());
        if (hit != null) {
            this.selectedNodeId = hit;
            this.pendingPurgeId = null;
            updateButtons();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInside(mouseX, mouseY, listX(), listY(), listWidth(), listHeight())) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (verticalAmount < 0) {
            this.scrollIndex = Math.min(maxScrollIndex(), this.scrollIndex + 1);
        } else if (verticalAmount > 0) {
            this.scrollIndex = Math.max(0, this.scrollIndex - 1);
        }
        return true;
    }

    private void drawEntries(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = listX();
        int y = listY();
        int width = listWidth();
        int height = listHeight();
        extractor.fill(x, y, x + width, y + height, 0x80101010);
        extractor.outline(x, y, width, height, 0xFF3F4A56);

        List<DeathNodeAdminPayload.Entry> entries = filteredEntries();
        if (entries.isEmpty()) {
            extractor.text(this.font, "目前篩選條件下沒有死亡節點。", x + 8, y + 10, 0xFFFFC857);
            return;
        }

        int visibleRows = visibleRows();
        int start = Math.min(this.scrollIndex, Math.max(0, entries.size() - visibleRows));
        int rowY = y + 4;
        for (int index = start; index < entries.size() && index < start + visibleRows; index++) {
            DeathNodeAdminPayload.Entry entry = entries.get(index);
            boolean selected = entry.id().equals(this.selectedNodeId);
            boolean hovered = isInside(mouseX, mouseY, x + 4, rowY, width - 12, ROW_HEIGHT - 4);
            extractor.fill(x + 4, rowY, x + width - 8, rowY + ROW_HEIGHT - 4,
                    selected ? 0xFF2D3F54 : hovered ? 0xC02A2F36 : 0x9020252B);
            extractor.outline(x + 4, rowY, width - 12, ROW_HEIGHT - 4, selected ? 0xFF78A6D6 : 0xFF343D47);

            int statusColor = "active".equals(entry.status()) ? 0xFF6AD98F : 0xFF9CA3AF;
            extractor.fill(x + 10, rowY + 8, x + 18, rowY + 16, statusColor);
            extractor.text(this.font, trimToWidth(entry.name(), width - 250), x + 24, rowY + 5, 0xFFFFFFFF);
            extractor.text(this.font, trimToWidth(entry.ownerName(), 104), x + width - 226, rowY + 5, 0xFFD2D8E0);
            extractor.text(this.font, statusText(entry.status()), x + width - 110, rowY + 5, statusColor);
            extractor.text(this.font, locationLine(entry), x + 24, rowY + 18, 0xFFB8C0C8);
            rowY += ROW_HEIGHT;
        }

        if (entries.size() > visibleRows) {
            drawScrollBar(extractor, x + width - 5, y + 4, height - 8, entries.size(), visibleRows, start);
        }
    }

    private void drawScrollBar(GuiGraphicsExtractor extractor, int x, int y, int height, int totalRows, int visibleRows, int start) {
        int thumbHeight = Math.max(16, height * visibleRows / Math.max(visibleRows, totalRows));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int maxStart = Math.max(1, totalRows - visibleRows);
        int thumbY = y + thumbTravel * start / maxStart;
        extractor.fill(x, y, x + 3, y + height, 0x80333333);
        extractor.fill(x, thumbY, x + 3, thumbY + thumbHeight, 0xFF9A9A9A);
    }

    private void cycleOwner(int direction) {
        List<OwnerOption> owners = ownerOptions();
        int optionCount = owners.size() + 1;
        if (optionCount <= 1) {
            this.ownerFilter = null;
            updateButtons();
            return;
        }

        int current = 0;
        if (this.ownerFilter != null) {
            for (int index = 0; index < owners.size(); index++) {
                if (owners.get(index).id().equals(this.ownerFilter)) {
                    current = index + 1;
                    break;
                }
            }
        }
        int next = Math.floorMod(current + direction, optionCount);
        this.ownerFilter = next == 0 ? null : owners.get(next - 1).id();
        resetFilterSelection();
    }

    private void cycleStatus() {
        this.statusFilter = this.statusFilter.next();
        resetFilterSelection();
    }

    private void resetFilterSelection() {
        this.scrollIndex = 0;
        this.pendingPurgeId = null;
        this.selectedNodeId = filteredEntries().stream().findFirst().map(DeathNodeAdminPayload.Entry::id).orElse(null);
        updateButtons();
    }

    private void requestRefresh() {
        if (ClientPlayNetworking.canSend(RequestDeathNodeAdminPayload.TYPE)) {
            ClientPlayNetworking.send(new RequestDeathNodeAdminPayload());
        }
    }

    private void disableSelected() {
        DeathNodeAdminPayload.Entry selected = selectedEntry();
        if (selected == null || !"active".equals(selected.status())) {
            return;
        }
        sendAction(selected.id(), DeathNodeAdminService.ACTION_DISABLE);
    }

    private void purgeSelected() {
        DeathNodeAdminPayload.Entry selected = selectedEntry();
        if (selected == null || "active".equals(selected.status())) {
            return;
        }
        if (!selected.id().equals(this.pendingPurgeId)) {
            this.pendingPurgeId = selected.id();
            updateButtons();
            return;
        }
        this.pendingPurgeId = null;
        sendAction(selected.id(), DeathNodeAdminService.ACTION_PURGE);
    }

    private void sendAction(UUID nodeId, String action) {
        if (ClientPlayNetworking.canSend(ManageDeathNodeAdminPayload.TYPE)) {
            ClientPlayNetworking.send(new ManageDeathNodeAdminPayload(nodeId, action));
        }
    }

    private void updateButtons() {
        if (this.previousOwnerButton != null) {
            this.previousOwnerButton.setX(ownerPreviousX());
            this.previousOwnerButton.setY(controlsY());
            this.previousOwnerButton.active = !ownerOptions().isEmpty();
        }
        if (this.ownerFilterButton != null) {
            this.ownerFilterButton.setX(ownerFilterX());
            this.ownerFilterButton.setY(controlsY());
            this.ownerFilterButton.setWidth(ownerFilterWidth());
            this.ownerFilterButton.setMessage(ownerFilterText());
            this.ownerFilterButton.active = !ownerOptions().isEmpty();
        }
        if (this.nextOwnerButton != null) {
            this.nextOwnerButton.setX(ownerNextX());
            this.nextOwnerButton.setY(controlsY());
            this.nextOwnerButton.active = !ownerOptions().isEmpty();
        }
        if (this.statusFilterButton != null) {
            this.statusFilterButton.setX(statusFilterX());
            this.statusFilterButton.setY(controlsY());
            this.statusFilterButton.setMessage(statusFilterText());
        }
        if (this.refreshButton != null) {
            this.refreshButton.setX(refreshX());
            this.refreshButton.setY(footerY());
        }
        DeathNodeAdminPayload.Entry selected = selectedEntry();
        if (this.disableButton != null) {
            this.disableButton.setX(disableX());
            this.disableButton.setY(footerY());
            this.disableButton.active = selected != null && "active".equals(selected.status());
        }
        if (this.purgeButton != null) {
            this.purgeButton.setX(purgeX());
            this.purgeButton.setY(footerY());
            this.purgeButton.setMessage(purgeButtonText());
            this.purgeButton.active = selected != null && !"active".equals(selected.status());
        }
        if (this.doneButton != null) {
            this.doneButton.setX(doneX());
            this.doneButton.setY(footerY());
        }
    }

    private List<DeathNodeAdminPayload.Entry> filteredEntries() {
        List<DeathNodeAdminPayload.Entry> entries = new ArrayList<>();
        for (DeathNodeAdminPayload.Entry entry : this.payload.entries()) {
            if (this.ownerFilter != null && !this.ownerFilter.equals(entry.ownerId())) {
                continue;
            }
            if (!this.statusFilter.matches(entry.status())) {
                continue;
            }
            entries.add(entry);
        }
        entries.sort(Comparator
                .comparing(DeathNodeAdminPayload.Entry::ownerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Comparator.comparingLong(DeathNodeAdminPayload.Entry::createdGameTime).reversed())
                .thenComparing(DeathNodeAdminPayload.Entry::id));
        return entries;
    }

    private List<OwnerOption> ownerOptions() {
        Map<UUID, String> owners = new LinkedHashMap<>();
        this.payload.entries().stream()
                .sorted(Comparator.comparing(DeathNodeAdminPayload.Entry::ownerName, String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> owners.putIfAbsent(entry.ownerId(), entry.ownerName()));
        List<OwnerOption> options = new ArrayList<>(owners.size());
        owners.forEach((id, name) -> options.add(new OwnerOption(id, name)));
        return options;
    }

    private DeathNodeAdminPayload.Entry selectedEntry() {
        if (this.selectedNodeId == null) {
            return null;
        }
        for (DeathNodeAdminPayload.Entry entry : filteredEntries()) {
            if (entry.id().equals(this.selectedNodeId)) {
                return entry;
            }
        }
        return null;
    }

    private UUID entryAt(double mouseX, double mouseY) {
        if (!isInside(mouseX, mouseY, listX(), listY(), listWidth(), listHeight())) {
            return null;
        }
        int relativeY = (int) mouseY - (listY() + 4);
        if (relativeY < 0) {
            return null;
        }
        List<DeathNodeAdminPayload.Entry> entries = filteredEntries();
        int index = Math.min(this.scrollIndex, maxScrollIndex()) + relativeY / ROW_HEIGHT;
        return index >= 0 && index < entries.size() ? entries.get(index).id() : null;
    }

    private Component ownerFilterText() {
        if (this.ownerFilter == null) {
            return Component.literal("玩家：全部");
        }
        for (OwnerOption owner : ownerOptions()) {
            if (owner.id().equals(this.ownerFilter)) {
                return Component.literal("玩家：" + owner.name());
            }
        }
        return Component.literal("玩家：全部");
    }

    private Component statusFilterText() {
        return Component.literal("狀態：" + this.statusFilter.label);
    }

    private Component purgeButtonText() {
        DeathNodeAdminPayload.Entry selected = selectedEntry();
        return Component.literal(selected != null && selected.id().equals(this.pendingPurgeId)
                ? "再次確認"
                : "永久刪除");
    }

    private String countSummary() {
        return "顯示 " + filteredEntries().size() + " / 共 " + this.payload.entries().size() + " 個節點";
    }

    private String locationLine(DeathNodeAdminPayload.Entry entry) {
        return shortDimension(entry.dimension()) + "  " + entry.x() + ", " + entry.y() + ", " + entry.z()
                + "  |  " + shortId(entry.id());
    }

    private String statusText(String status) {
        return switch (status) {
            case "active" -> "ACTIVE";
            case "disabled" -> "DISABLED";
            default -> status.toUpperCase(java.util.Locale.ROOT);
        };
    }

    private int maxScrollIndex() {
        return Math.max(0, filteredEntries().size() - visibleRows());
    }

    private int visibleRows() {
        return Math.max(1, (listHeight() - 8) / ROW_HEIGHT);
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(330, this.width - 12));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(250, this.height - 12));
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private int controlsY() {
        return panelY() + HEADER_HEIGHT + 3;
    }

    private int listX() {
        return panelX() + PANEL_PADDING;
    }

    private int listY() {
        return panelY() + HEADER_HEIGHT + CONTROL_HEIGHT;
    }

    private int listWidth() {
        return panelWidth() - PANEL_PADDING * 2;
    }

    private int listHeight() {
        return panelHeight() - HEADER_HEIGHT - CONTROL_HEIGHT - FOOTER_HEIGHT;
    }

    private int ownerPreviousX() {
        return panelX() + PANEL_PADDING;
    }

    private int ownerFilterX() {
        return ownerPreviousX() + 24;
    }

    private int ownerFilterWidth() {
        return Math.max(120, Math.min(230, panelWidth() - 260));
    }

    private int ownerNextX() {
        return ownerFilterX() + ownerFilterWidth() + 4;
    }

    private int statusFilterX() {
        return ownerNextX() + 28;
    }

    private int footerY() {
        return panelY() + panelHeight() - 25;
    }

    private int refreshX() {
        return panelX() + PANEL_PADDING;
    }

    private int doneX() {
        return panelX() + panelWidth() - PANEL_PADDING - 62;
    }

    private int purgeX() {
        return doneX() - 6 - 88;
    }

    private int disableX() {
        return purgeX() - 6 - 72;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String shortDimension(String dimension) {
        int index = dimension.indexOf(':');
        return index >= 0 && index + 1 < dimension.length() ? dimension.substring(index + 1) : dimension;
    }

    private static String shortId(UUID id) {
        String value = id.toString();
        return value.length() <= 8 ? value : value.substring(0, 8);
    }

    private String trimToWidth(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        String trimmed = value;
        while (!trimmed.isEmpty() && this.font.width(trimmed) + ellipsisWidth > width) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private record OwnerOption(UUID id, String name) {
    }

    private enum StatusFilter {
        ALL("全部"),
        ACTIVE("ACTIVE"),
        DISABLED("DISABLED");

        private final String label;

        StatusFilter(String label) {
            this.label = label;
        }

        private StatusFilter next() {
            return switch (this) {
                case ALL -> ACTIVE;
                case ACTIVE -> DISABLED;
                case DISABLED -> ALL;
            };
        }

        private boolean matches(String status) {
            return this == ALL || this.name().equalsIgnoreCase(status);
        }
    }
}
