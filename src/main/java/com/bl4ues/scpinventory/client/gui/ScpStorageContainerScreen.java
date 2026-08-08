package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.ClientNetwork;
import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.client.gui.components.StorageListView;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.container.StorageContainerSupport;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.StorageContainerTransferPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;
import java.util.List;
import java.util.Locale;

/**
 * Two-pane SCP Inventory presentation for plain storage menus.
 *
 * <p>The server-owned menu remains open and authoritative. Only its vanilla
 * screen is replaced; storage slot synchronization continues through the normal
 * container protocol, while transfers to and from the SCP capability inventory
 * use {@link StorageContainerTransferPacket}.</p>
 */
public final class ScpStorageContainerScreen extends Screen {
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_SELECTED = 0xFF202020;
    private static final int TAB_ACTIVE = 0x55B2B3B3;
    private static final int TAB_INACTIVE = 0x336A6C6C;
    private static final int PANEL_BACKGROUND = 0x8F545D5F;
    private static final int PANEL_BORDER = 0x666A6C6C;
    private static final int DRAG_ICON_BOX = 0x99303638;
    private static final int DRAG_ICON_CORNER = 0xCC6A6C6C;

    private static final int ROW_HEIGHT = 40;
    private static final int TAB_HEIGHT = 17;
    private static final int DRAG_ICON_FRAME_SIZE = 24;
    private static final long DOUBLE_CLICK_WINDOW_MS = 320L;
    private static final double DRAG_THRESHOLD = 4.0D;

    private enum BackpackTab {
        INVENTORY("INVENTORY", StorageContainerTransferPacket.SECTION_MAIN),
        KEYS("KEYS", StorageContainerTransferPacket.SECTION_KEYS),
        CODEX("CODEX", StorageContainerTransferPacket.SECTION_CODEX);

        private final String label;
        private final int packetSection;

        BackpackTab(String label, int packetSection) {
            this.label = label;
            this.packetSection = packetSection;
        }
    }

    private enum DragOrigin {
        NONE,
        BACKPACK,
        CONTAINER
    }

    private final AbstractContainerMenu menu;
    private final Component containerName;
    private final int openedContainerId;

    private IScpInventory inventory;
    private List<Integer> storageSlotIds = List.of();
    private StorageListView backpackList;
    private StorageListView containerList;
    private List<ItemStack> containerItems = List.of();

    private BackpackTab backpackTab = BackpackTab.INVENTORY;
    private int inventoryScroll;
    private int keysScroll;
    private int codexScroll;
    private int containerScroll;

    private int leftPanelX;
    private int rightPanelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int titleY;
    private int listY;
    private int listWidth;
    private int visibleRows;
    private int footerY;

    private DragOrigin dragOrigin = DragOrigin.NONE;
    private BackpackTab dragBackpackTab = BackpackTab.INVENTORY;
    private int dragSourceIndex = -1;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private double dragStartX;
    private double dragStartY;
    private boolean dragMoved;

    private DragOrigin lastClickOrigin = DragOrigin.NONE;
    private BackpackTab lastClickTab = BackpackTab.INVENTORY;
    private int lastClickIndex = -1;
    private long lastClickTime;

    public ScpStorageContainerScreen(AbstractContainerMenu menu,
                                     Component containerName) {
        super(containerName == null
                ? Component.literal("Storage") : containerName);
        this.menu = menu;
        this.containerName = containerName == null
                ? Component.literal("Storage") : containerName;
        this.openedContainerId = menu.containerId;
    }

    @Override
    protected void init() {
        rememberScrollPositions();
        computeLayout();

        if (minecraft == null || minecraft.player == null) {
            return;
        }

        storageSlotIds = StorageContainerSupport.storageSlotIds(
                menu, minecraft.player.getInventory());
        if (storageSlotIds.isEmpty()) {
            minecraft.setScreen(null);
            return;
        }

        containerItems = new MenuSlotItemList(menu, storageSlotIds);
        containerList = new StorageListView(
                rightPanelX + 14, listY, listWidth, visibleRows,
                containerItems, null);
        containerList.setScrollOffset(containerScroll);

        minecraft.player.getCapability(ScpInventoryCapability.INSTANCE)
                .ifPresent(found -> {
                    inventory = found;
                    rebuildBackpackList();
                });

        ClientNetwork.requestInventorySync();
    }

    private void computeLayout() {
        int horizontalMargin = Mth.clamp(width / 36, 12, 30);
        int availableWidth = Math.max(320, width - horizontalMargin * 2);
        int totalWidth = Math.min(1120, availableWidth);
        int gap = Mth.clamp(totalWidth / 30, 16, 34);

        panelWidth = Math.max(150, (totalWidth - gap) / 2);
        int actualTotal = panelWidth * 2 + gap;
        leftPanelX = (width - actualTotal) / 2;
        rightPanelX = leftPanelX + panelWidth + gap;

        int usableRows = (height - 132) / ROW_HEIGHT;
        visibleRows = Mth.clamp(usableRows, 3, 7);
        panelHeight = visibleRows * ROW_HEIGHT + 62;

        int groupHeight = 22 + panelHeight + 28;
        titleY = Math.max(12, (height - groupHeight) / 2);
        panelY = titleY + 20;
        listY = panelY + 46;
        footerY = panelY + panelHeight + 10;
        listWidth = Math.max(90, panelWidth - 38);
    }

    private void rebuildBackpackList() {
        if (inventory == null) {
            backpackList = null;
            return;
        }

        List<ItemStack> items;
        String fixedLabel;
        int restoredScroll;

        switch (backpackTab) {
            case KEYS -> {
                items = inventory.getKeys();
                fixedLabel = "Key";
                restoredScroll = keysScroll;
            }
            case CODEX -> {
                items = inventory.getDocuments();
                fixedLabel = "Document";
                restoredScroll = codexScroll;
            }
            default -> {
                items = inventory.getInventory();
                fixedLabel = null;
                restoredScroll = inventoryScroll;
            }
        }

        backpackList = new StorageListView(
                leftPanelX + 14, listY, listWidth, visibleRows,
                items, fixedLabel);
        backpackList.setScrollOffset(restoredScroll);
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (!InventoryModuleRuntimeState.isEnabledForClient()
                || minecraft.player.containerMenu != menu
                || minecraft.player.containerMenu.containerId
                != openedContainerId
                || !StorageContainerSupport.isSupported(
                menu, minecraft.player.getInventory())) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderPanels(graphics);
        renderHeaders(graphics);
        renderTabs(graphics);

        if (backpackList != null) {
            backpackList.render(graphics, mouseX, mouseY);
        }
        if (containerList != null) {
            containerList.render(graphics, mouseX, mouseY);
        }

        renderEmptyHints(graphics);
        renderFooter(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderDraggedStack(graphics, mouseX, mouseY);
    }

    private void renderPanels(GuiGraphics graphics) {
        drawPanel(graphics, leftPanelX, panelY, panelWidth, panelHeight);
        drawPanel(graphics, rightPanelX, panelY, panelWidth, panelHeight);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y,
                           int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height,
                PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height,
                PANEL_BORDER);
    }

    private void renderHeaders(GuiGraphics graphics) {
        drawSectionTitle(graphics, leftPanelX, titleY,
                "://INVENTORY_", "BACKPACK");
        drawBackpackCount(graphics);

        String containerPrefix = "://CONTAINER_";
        int titleBudget = Math.max(48, panelWidth
                - containerCountWidth() - 12);
        String containerSuffix = trimSectionSuffix(containerPrefix,
                stylizedContainerName(), titleBudget);
        drawSectionTitle(graphics, rightPanelX, titleY,
                containerPrefix, containerSuffix);
        drawContainerCount(graphics);
    }

    private int containerCountWidth() {
        int occupied = containerList == null
                ? countNonEmpty(containerItems)
                : containerList.nonEmptyCount();
        String primary = Integer.toString(occupied);
        String suffix = " of " + storageSlotIds.size() + " slots";
        return minecraft.font.width(ScpFonts.roboto(primary))
                + minecraft.font.width(ScpFonts.roboto(suffix));
    }

    private String trimSectionSuffix(String prefix, String suffix,
                                     int totalBudget) {
        int available = totalBudget
                - minecraft.font.width(ScpFonts.roboto(prefix));
        if (available <= 0) {
            return "";
        }
        if (minecraft.font.width(ScpFonts.roboto(suffix)) <= available) {
            return suffix;
        }
        String ellipsis = "...";
        int ellipsisWidth = minecraft.font.width(ScpFonts.roboto(ellipsis));
        if (available <= ellipsisWidth) {
            return ellipsis;
        }
        return minecraft.font.plainSubstrByWidth(
                suffix, available - ellipsisWidth).trim() + ellipsis;
    }

    private void drawSectionTitle(GuiGraphics graphics, int x, int y,
                                  String prefix, String suffix) {
        graphics.drawString(minecraft.font, ScpFonts.roboto(prefix),
                x, y, TEXT_GRAY, false);
        int suffixX = x + minecraft.font.width(ScpFonts.roboto(prefix));
        graphics.drawString(minecraft.font, ScpFonts.roboto(suffix),
                suffixX, y, TEXT_WHITE, false);
    }

    private void drawBackpackCount(GuiGraphics graphics) {
        String primary;
        String suffix;

        if (inventory == null) {
            primary = "0";
            suffix = " items";
        } else {
            switch (backpackTab) {
                case KEYS -> {
                    primary = Integer.toString(inventory.getKeyCount());
                    suffix = " of " + IScpInventory.MAX_KEY_COUNT + " keys";
                }
                case CODEX -> {
                    primary = Integer.toString(countNonEmpty(
                            inventory.getDocuments()));
                    suffix = " documents";
                }
                default -> {
                    primary = Integer.toString(
                            inventory.getInventoryCount());
                    suffix = " of " + inventory.getMaxMainSlots()
                            + " items";
                }
            }
        }

        drawRightAlignedCount(graphics, leftPanelX, primary, suffix);
    }

    private void drawContainerCount(GuiGraphics graphics) {
        int occupied = containerList == null
                ? countNonEmpty(containerItems)
                : containerList.nonEmptyCount();
        drawRightAlignedCount(graphics, rightPanelX,
                Integer.toString(occupied),
                " of " + storageSlotIds.size() + " slots");
    }

    private void drawRightAlignedCount(GuiGraphics graphics, int panelX,
                                       String primary, String suffix) {
        int totalWidth = minecraft.font.width(ScpFonts.roboto(primary))
                + minecraft.font.width(ScpFonts.roboto(suffix));
        int x = panelX + panelWidth - totalWidth;
        graphics.drawString(minecraft.font, ScpFonts.roboto(primary),
                x, titleY, TEXT_WHITE, false);
        graphics.drawString(minecraft.font, ScpFonts.roboto(suffix),
                x + minecraft.font.width(ScpFonts.roboto(primary)),
                titleY, TEXT_GRAY, false);
    }

    private void renderTabs(GuiGraphics graphics) {
        int gap = 8;
        int innerWidth = panelWidth - 28;
        int tabWidth = Math.max(44, (innerWidth - gap * 2) / 3);
        int totalWidth = tabWidth * 3 + gap * 2;
        int x = leftPanelX + (panelWidth - totalWidth) / 2;
        int y = panelY + 12;

        for (BackpackTab tab : BackpackTab.values()) {
            drawTab(graphics, x, y, tabWidth, tab.label,
                    tab == backpackTab);
            x += tabWidth + gap;
        }
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int width,
                         String label, boolean active) {
        graphics.fill(x, y, x + width, y + TAB_HEIGHT,
                active ? TAB_ACTIVE : TAB_INACTIVE);
        Component text = ScpFonts.roboto(label);
        int textX = x + (width - minecraft.font.width(text)) / 2;
        graphics.drawString(minecraft.font, text, textX, y + 5,
                active ? TEXT_SELECTED : TEXT_WHITE, false);
    }

    private void renderEmptyHints(GuiGraphics graphics) {
        int centerY = listY + visibleRows * ROW_HEIGHT / 2 - 4;
        if (backpackList != null && backpackList.nonEmptyCount() == 0) {
            drawCentered(graphics, "EMPTY", leftPanelX + panelWidth / 2,
                    centerY, TEXT_GRAY);
        }
        if (containerList != null && containerList.nonEmptyCount() == 0) {
            drawCentered(graphics, "EMPTY", rightPanelX + panelWidth / 2,
                    centerY, TEXT_GRAY);
        }
    }

    private void renderFooter(GuiGraphics graphics) {
        String hint = "Double-click or Shift + Right Click to transfer"
                + "   ·   Drag between panels";
        int budget = Math.max(120, width - 24);
        if (minecraft.font.width(ScpFonts.roboto(hint)) > budget) {
            hint = "Double-click / Shift+RMB / Drag to transfer";
        }
        if (minecraft.font.width(ScpFonts.roboto(hint)) > budget) {
            hint = "Transfer: double-click or drag";
        }
        drawCentered(graphics, hint, width / 2, footerY, TEXT_GRAY);
    }

    private void drawCentered(GuiGraphics graphics, String text,
                              int centerX, int y, int color) {
        Component styled = ScpFonts.roboto(text);
        graphics.drawString(minecraft.font, styled,
                centerX - minecraft.font.width(styled) / 2,
                y, color, false);
    }

    private String stylizedContainerName() {
        String raw = containerName.getString().trim();
        if (raw.isEmpty()) {
            return "STORAGE";
        }

        StringBuilder result = new StringBuilder();
        boolean lastUnderscore = false;
        for (int i = 0; i < raw.length(); i++) {
            char character = Character.toUpperCase(raw.charAt(i));
            if (Character.isLetterOrDigit(character)) {
                result.append(character);
                lastUnderscore = false;
            } else if (!lastUnderscore && result.length() > 0) {
                result.append('_');
                lastUnderscore = true;
            }
        }

        while (result.length() > 0
                && result.charAt(result.length() - 1) == '_') {
            result.deleteCharAt(result.length() - 1);
        }

        return result.length() == 0
                ? "STORAGE"
                : result.toString().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double delta) {
        if (backpackList != null
                && backpackList.isMouseOver(mouseX, mouseY)) {
            return backpackList.mouseScrolled(delta);
        }
        if (containerList != null
                && containerList.isMouseOver(mouseX, mouseY)) {
            return containerList.mouseScrolled(delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (backpackList != null
                && backpackList.mouseClickedScrollbar(
                mouseX, mouseY, button)) {
            return true;
        }
        if (containerList != null
                && containerList.mouseClickedScrollbar(
                mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && clickTab(mouseX, mouseY)) {
            return true;
        }

        boolean quickTransfer = button == 1 && hasShiftDown();

        if (backpackList != null) {
            int index = backpackList.getClickedIndex(mouseX, mouseY);
            if (index >= 0) {
                if (quickTransfer) {
                    transferBackpackToContainer(index, -1);
                    resetDoubleClick();
                    return true;
                }
                if (button == 0) {
                    if (isDoubleClick(DragOrigin.BACKPACK,
                            backpackTab, index)) {
                        transferBackpackToContainer(index, -1);
                        resetDoubleClick();
                        return true;
                    }
                    startBackpackDrag(index, mouseX, mouseY);
                    return true;
                }
            }
        }

        if (containerList != null) {
            int relativeIndex = containerList.getClickedIndex(
                    mouseX, mouseY);
            if (relativeIndex >= 0) {
                int menuSlot = menuSlotId(relativeIndex);
                if (menuSlot < 0) {
                    return false;
                }
                if (quickTransfer) {
                    transferContainerToBackpack(menuSlot);
                    resetDoubleClick();
                    return true;
                }
                if (button == 0) {
                    if (isDoubleClick(DragOrigin.CONTAINER,
                            backpackTab, menuSlot)) {
                        transferContainerToBackpack(menuSlot);
                        resetDoubleClick();
                        return true;
                    }
                    startContainerDrag(menuSlot, mouseX, mouseY);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button == 0 && backpackList != null
                && backpackList.mouseDraggedScrollbar(mouseY)) {
            return true;
        }
        if (button == 0 && containerList != null
                && containerList.mouseDraggedScrollbar(mouseY)) {
            return true;
        }

        if (button == 0 && dragOrigin != DragOrigin.NONE) {
            if (Math.abs(mouseX - dragStartX) > DRAG_THRESHOLD
                    || Math.abs(mouseY - dragStartY) > DRAG_THRESHOLD) {
                dragMoved = true;
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (backpackList != null
                && backpackList.mouseReleasedScrollbar(button)) {
            return true;
        }
        if (containerList != null
                && containerList.mouseReleasedScrollbar(button)) {
            return true;
        }

        if (button == 0 && dragOrigin != DragOrigin.NONE) {
            if (dragMoved) {
                finishDrag(mouseX, mouseY);
            }
            clearDrag();
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean clickTab(double mouseX, double mouseY) {
        int gap = 8;
        int innerWidth = panelWidth - 28;
        int tabWidth = Math.max(44, (innerWidth - gap * 2) / 3);
        int totalWidth = tabWidth * 3 + gap * 2;
        int x = leftPanelX + (panelWidth - totalWidth) / 2;
        int y = panelY + 12;

        if (mouseY < y || mouseY > y + TAB_HEIGHT) {
            return false;
        }

        for (BackpackTab tab : BackpackTab.values()) {
            if (mouseX >= x && mouseX <= x + tabWidth) {
                rememberBackpackScroll();
                backpackTab = tab;
                rebuildBackpackList();
                clearDrag();
                resetDoubleClick();
                return true;
            }
            x += tabWidth + gap;
        }
        return false;
    }

    private void startBackpackDrag(int index,
                                   double mouseX, double mouseY) {
        if (inventory == null) {
            return;
        }
        ItemStack stack = backpackStack(backpackTab, index);
        if (stack.isEmpty()) {
            return;
        }

        dragOrigin = DragOrigin.BACKPACK;
        dragBackpackTab = backpackTab;
        dragSourceIndex = index;
        draggedStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragMoved = false;
    }

    private void startContainerDrag(int menuSlot,
                                    double mouseX, double mouseY) {
        if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
            return;
        }
        ItemStack stack = menu.slots.get(menuSlot).getItem();
        if (stack.isEmpty()) {
            return;
        }

        dragOrigin = DragOrigin.CONTAINER;
        dragSourceIndex = menuSlot;
        draggedStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragMoved = false;
    }

    private void finishDrag(double mouseX, double mouseY) {
        if (dragOrigin == DragOrigin.BACKPACK
                && insidePanel(mouseX, mouseY,
                rightPanelX, panelY, panelWidth, panelHeight)) {
            int target = -1;
            if (containerList != null) {
                int relative = containerList.getClickedIndex(
                        mouseX, mouseY);
                target = menuSlotId(relative);
            }
            transferBackpackToContainer(dragSourceIndex, target,
                    dragBackpackTab);
            return;
        }

        if (dragOrigin == DragOrigin.CONTAINER
                && insidePanel(mouseX, mouseY,
                leftPanelX, panelY, panelWidth, panelHeight)) {
            transferContainerToBackpack(dragSourceIndex);
        }
    }

    private void transferBackpackToContainer(int sourceIndex,
                                             int targetMenuSlot) {
        transferBackpackToContainer(sourceIndex, targetMenuSlot,
                backpackTab);
    }

    private void transferBackpackToContainer(int sourceIndex,
                                             int targetMenuSlot,
                                             BackpackTab sourceTab) {
        ModNetwork.CHANNEL.sendToServer(
                StorageContainerTransferPacket.backpackToContainer(
                        openedContainerId, sourceTab.packetSection,
                        sourceIndex, targetMenuSlot));
    }

    private void transferContainerToBackpack(int sourceMenuSlot) {
        ModNetwork.CHANNEL.sendToServer(
                StorageContainerTransferPacket.containerToBackpack(
                        openedContainerId, sourceMenuSlot));
    }

    private ItemStack backpackStack(BackpackTab tab, int index) {
        if (inventory == null || index < 0) {
            return ItemStack.EMPTY;
        }

        return switch (tab) {
            case INVENTORY -> inventory.isValidMainSlot(index)
                    ? inventory.getInventoryItem(index)
                    : ItemStack.EMPTY;
            case KEYS -> index < inventory.getKeys().size()
                    ? inventory.getKeys().get(index)
                    : ItemStack.EMPTY;
            case CODEX -> index < inventory.getDocuments().size()
                    ? inventory.getDocumentItem(index)
                    : ItemStack.EMPTY;
        };
    }

    private int menuSlotId(int relativeStorageIndex) {
        if (relativeStorageIndex < 0
                || relativeStorageIndex >= storageSlotIds.size()) {
            return -1;
        }
        return storageSlotIds.get(relativeStorageIndex);
    }

    private boolean isDoubleClick(DragOrigin origin,
                                  BackpackTab tab, int index) {
        long now = System.currentTimeMillis();
        boolean doubleClick = origin == lastClickOrigin
                && tab == lastClickTab
                && index == lastClickIndex
                && now - lastClickTime <= DOUBLE_CLICK_WINDOW_MS;

        lastClickOrigin = origin;
        lastClickTab = tab;
        lastClickIndex = index;
        lastClickTime = now;
        return doubleClick;
    }

    private void resetDoubleClick() {
        lastClickOrigin = DragOrigin.NONE;
        lastClickTab = BackpackTab.INVENTORY;
        lastClickIndex = -1;
        lastClickTime = 0L;
    }

    private void clearDrag() {
        dragOrigin = DragOrigin.NONE;
        dragSourceIndex = -1;
        draggedStack = ItemStack.EMPTY;
        dragMoved = false;
        dragStartX = 0.0D;
        dragStartY = 0.0D;
    }

    private void renderDraggedStack(GuiGraphics graphics,
                                    int mouseX, int mouseY) {
        if (!dragMoved || draggedStack.isEmpty()) {
            return;
        }

        int x = mouseX - DRAG_ICON_FRAME_SIZE / 2;
        int y = mouseY - DRAG_ICON_FRAME_SIZE / 2;
        int right = x + DRAG_ICON_FRAME_SIZE;
        int bottom = y + DRAG_ICON_FRAME_SIZE;
        int corner = 6;

        graphics.fill(x, y, right, bottom, DRAG_ICON_BOX);
        graphics.fill(x, y, x + corner, y + 1, DRAG_ICON_CORNER);
        graphics.fill(x, y, x + 1, y + corner, DRAG_ICON_CORNER);
        graphics.fill(right - corner, y, right, y + 1,
                DRAG_ICON_CORNER);
        graphics.fill(right - 1, y, right, y + corner,
                DRAG_ICON_CORNER);
        graphics.fill(x, bottom - 1, x + corner, bottom,
                DRAG_ICON_CORNER);
        graphics.fill(x, bottom - corner, x + 1, bottom,
                DRAG_ICON_CORNER);
        graphics.fill(right - corner, bottom - 1, right, bottom,
                DRAG_ICON_CORNER);
        graphics.fill(right - 1, bottom - corner, right, bottom,
                DRAG_ICON_CORNER);

        graphics.renderItem(draggedStack, x + 4, y + 4);
        graphics.renderItemDecorations(minecraft.font, draggedStack,
                x + 4, y + 4);
    }

    private boolean insidePanel(double mouseX, double mouseY,
                                int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private void rememberScrollPositions() {
        if (containerList != null) {
            containerScroll = containerList.getScrollOffset();
        }
        rememberBackpackScroll();
    }

    private void rememberBackpackScroll() {
        if (backpackList == null) {
            return;
        }

        switch (backpackTab) {
            case INVENTORY ->
                    inventoryScroll = backpackList.getScrollOffset();
            case KEYS -> keysScroll = backpackList.getScrollOffset();
            case CODEX -> codexScroll = backpackList.getScrollOffset();
        }
    }

    private int countNonEmpty(List<ItemStack> stacks) {
        int count = 0;
        if (stacks == null) {
            return 0;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null
                && minecraft.options.keyInventory.matches(
                keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        rememberScrollPositions();
        if (minecraft != null && minecraft.player != null
                && minecraft.player.containerMenu == menu) {
            minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class MenuSlotItemList
            extends AbstractList<ItemStack> {
        private final AbstractContainerMenu menu;
        private final List<Integer> slotIds;

        private MenuSlotItemList(AbstractContainerMenu menu,
                                 List<Integer> slotIds) {
            this.menu = menu;
            this.slotIds = slotIds;
        }

        @Override
        public ItemStack get(int index) {
            if (index < 0 || index >= slotIds.size()) {
                return ItemStack.EMPTY;
            }
            int menuSlot = slotIds.get(index);
            if (menuSlot < 0 || menuSlot >= menu.slots.size()) {
                return ItemStack.EMPTY;
            }
            return menu.slots.get(menuSlot).getItem();
        }

        @Override
        public int size() {
            return slotIds.size();
        }
    }
}
