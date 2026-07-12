package net.mcreator.scpadditions.inventory.client;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.inventory.ScpInventoryActionPacket;
import net.mcreator.scpadditions.inventory.ScpInventoryActionPacket.Action;
import net.mcreator.scpadditions.vitals.client.PlayerVitalsClient;

import java.util.ArrayList;
import java.util.List;

/** Integrated final-layout SCP Inventory with functional server-side actions. */
public final class ScpInventoryScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            "scpinventory", "textures/gui/inventory_background.png");
    private static final ResourceLocation INVENTORY_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/inventoryicon.png");
    private static final ResourceLocation INVENTORY_ICON_SELECTED = new ResourceLocation(
            "scpinventory", "textures/gui/inventoryicon_selected.png");
    private static final ResourceLocation STATUS_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/statusicon.png");
    private static final ResourceLocation STATUS_ICON_SELECTED = new ResourceLocation(
            "scpinventory", "textures/gui/statusicon_selected.png");
    private static final ResourceLocation CODEX_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/codexicon.png");
    private static final ResourceLocation CODEX_ICON_SELECTED = new ResourceLocation(
            "scpinventory", "textures/gui/codexicon_selected.png");
    private static final ResourceLocation HEALTH_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/health.png");
    private static final ResourceLocation ROBOTO = new ResourceLocation(
            "scpinventory", "roboto");
    private static final ResourceLocation MONTSERRAT = new ResourceLocation(
            "scpinventory", "montserrat");

    private static final int SOURCE_WIDTH = 1406;
    private static final int SOURCE_HEIGHT = 1080;
    private static final int TEXT = 0xFFB2B3B3;
    private static final int TEXT_DIM = 0xFF6A6C6C;
    private static final int TEXT_BRIGHT = 0xFFE8E8E8;
    private static final int PANEL = 0x8F545D5F;
    private static final int PANEL_DARK = 0xB3262C2E;
    private static final int ROW = 0xA8343A3C;
    private static final int ROW_HOVER = 0xC64A5356;
    private static final int ROW_SELECTED = 0xD0AAB1B2;
    private static final int LINE = 0x886A6C6C;
    private static final int BUTTON = 0xBB3A4244;
    private static final int BUTTON_HOVER = 0xDD596367;
    private static final int BUTTON_DISABLED = 0x77404749;
    private static final int HEALTH = 0xCC7EA38A;
    private static final int STAMINA = 0xCC7EA0B7;

    private static final int ROW_HEIGHT = 34;
    private static final int NAV_ICON_SIZE = 24;
    private static final int NAV_WIDTH = 116;
    private static final int ACTION_HEIGHT = 22;
    private static final long DOUBLE_CLICK_MS = 320L;

    private enum Mode { INVENTORY, STATUS, CODEX }
    private enum Selection { NONE, MAIN, KEY, EQUIPMENT, DOCUMENT }

    private IScpInventory inventory;
    private Mode mode = Mode.INVENTORY;
    private boolean showingKeys;
    private Selection selection = Selection.NONE;
    private int selectedIndex = -1;
    private ScpEquipmentSlot selectedEquipment;
    private int scroll;

    private int rootX, rootY, rootWidth, rootHeight;
    private int titleY, tabY, navY;
    private int leftX, leftY, leftWidth, panelHeight;
    private int rightX, rightY, rightWidth;
    private int inventoryTabX, keysTabX, tabWidth;
    private long lastClickAt;
    private int lastClickIndex = -1;
    private Selection lastClickSelection = Selection.NONE;

    public ScpInventoryScreen() {
        super(Component.translatable("screen.scp_additions.scp_inventory"));
    }

    @Override
    protected void init() {
        computeLayout();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.getCapability(ScpInventoryCapability.INSTANCE)
                    .ifPresent(value -> inventory = value);
        }
    }

    private void computeLayout() {
        int margin = 24;
        int availableWidth = Math.max(1, width - margin * 2);
        int availableHeight = Math.max(1, height - margin * 2);
        float aspect = SOURCE_WIDTH / (float) SOURCE_HEIGHT;
        rootHeight = availableHeight;
        rootWidth = Math.round(rootHeight * aspect);
        if (rootWidth > availableWidth) {
            rootWidth = availableWidth;
            rootHeight = Math.round(rootWidth / aspect);
        }
        rootX = (width - rootWidth) / 2;
        rootY = (height - rootHeight) / 2;
        titleY = rootY + Math.round(rootHeight * 0.105F);
        tabY = titleY + Math.round(rootHeight * 0.043F);
        navY = rootY + rootHeight - Math.round(rootHeight * 0.120F);

        int side = Math.round(rootWidth * 0.055F);
        int gap = Math.round(rootWidth * 0.040F);
        int shared = (rootWidth - side * 2 - gap) / 2;
        leftX = rootX + side;
        rightX = leftX + shared + gap;
        leftWidth = shared;
        rightWidth = shared;
        leftY = tabY - 5;
        rightY = leftY;
        int bottom = navY - Math.round(rootHeight * 0.035F);
        panelHeight = Math.max(160, bottom - leftY);
        tabWidth = Math.max(82, Math.round(leftWidth * 0.34F));
        inventoryTabX = leftX + 14;
        keysTabX = inventoryTabX + tabWidth + 8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        RenderSystem.enableBlend();
        graphics.blit(BACKGROUND, rootX, rootY, rootWidth, rootHeight,
                0.0F, 0.0F, SOURCE_WIDTH, SOURCE_HEIGHT,
                SOURCE_WIDTH, SOURCE_HEIGHT);
        RenderSystem.disableBlend();
        graphics.fill(rootX, navY - 16, rootX + rootWidth,
                rootY + rootHeight, 0xB0262D2F);

        renderTopStatus(graphics);
        switch (mode) {
            case INVENTORY -> renderInventory(graphics, mouseX, mouseY);
            case STATUS -> renderStatus(graphics, mouseX, mouseY);
            case CODEX -> renderCodex(graphics, mouseX, mouseY);
        }
        renderNavigation(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTopStatus(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) return;
        int x = rootX + Math.round(rootWidth * 0.038F);
        int y = rootY + Math.round(rootHeight * 0.032F);
        drawIcon(graphics, HEALTH_ICON, x, y - 1, 20);
        int health = Math.round(minecraft.player.getHealth());
        int max = Math.max(1, Math.round(minecraft.player.getMaxHealth()));
        drawText(graphics, "HEALTH", x + 27, y, TEXT_BRIGHT, 0.86F);
        drawText(graphics, Math.round(health / (float) max * 100.0F) + "/100",
                x + 27, y + 13, TEXT_BRIGHT, 0.86F);
    }

    private void renderInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSectionTitle(graphics, leftX, titleY, "BACKPACK");
        drawSectionTitle(graphics, rightX, titleY, "EQUIPMENT");
        graphics.fill(leftX, leftY, leftX + leftWidth,
                leftY + panelHeight, PANEL);
        graphics.fill(rightX, rightY, rightX + rightWidth,
                rightY + panelHeight, PANEL);
        renderTabs(graphics, mouseX, mouseY);

        if (inventory == null) {
            graphics.drawCenteredString(font, montserrat("Synchronizing..."),
                    leftX + leftWidth / 2, leftY + 46, TEXT_DIM);
            return;
        }

        if (showingKeys) renderKeys(graphics, mouseX, mouseY);
        else renderMainItems(graphics, mouseX, mouseY);
        renderEquipment(graphics, mouseX, mouseY);
        renderActions(graphics, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int h = 18;
        drawTab(graphics, inventoryTabX, tabY, tabWidth, h,
                "INVENTORY", !showingKeys, mouseX, mouseY);
        drawTab(graphics, keysTabX, tabY, tabWidth, h,
                "KEYS", showingKeys, mouseX, mouseY);
        if (inventory != null) {
            String count = showingKeys
                    ? inventory.getKeyCount() + "/" + IScpInventory.MAX_KEY_COUNT
                    : inventory.getInventoryCount() + "/" + inventory.getMaxMainSlots();
            graphics.drawString(font, roboto(count),
                    leftX + leftWidth - font.width(count) - 14,
                    titleY, TEXT_DIM, false);
        }
    }

    private void renderMainItems(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Integer> slots = nonEmptyMainSlots();
        int listY = tabY + 27;
        int actionSpace = ACTION_HEIGHT + 22;
        int visible = Math.max(1, (panelHeight - (listY - leftY)
                - actionSpace) / ROW_HEIGHT);
        clampScroll(slots.size(), visible);
        int end = Math.min(slots.size(), scroll + visible);
        for (int row = scroll; row < end; row++) {
            int slot = slots.get(row);
            drawItemRow(graphics, leftX + 12,
                    listY + (row - scroll) * ROW_HEIGHT,
                    leftWidth - 24, inventory.getInventoryItem(slot),
                    ScpItemClassifier.getDisplayType(inventory.getInventoryItem(slot)),
                    selection == Selection.MAIN && selectedIndex == slot,
                    mouseX, mouseY);
        }
        if (slots.isEmpty()) drawEmpty(graphics, leftX, listY, leftWidth,
                "No items stored");
    }

    private void renderKeys(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ItemStack> keys = inventory.getKeys();
        int listY = tabY + 27;
        int visible = Math.max(1, (panelHeight - (listY - leftY)
                - ACTION_HEIGHT - 22) / ROW_HEIGHT);
        clampScroll(keys.size(), visible);
        int end = Math.min(keys.size(), scroll + visible);
        for (int row = scroll; row < end; row++) {
            ItemStack stack = keys.get(row);
            drawItemRow(graphics, leftX + 12,
                    listY + (row - scroll) * ROW_HEIGHT,
                    leftWidth - 24, stack, "Key",
                    selection == Selection.KEY && selectedIndex == row,
                    mouseX, mouseY);
        }
        if (keys.isEmpty()) drawEmpty(graphics, leftX, listY, leftWidth,
                "No keys stored");
    }

    private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inventory == null) return;
        ScpEquipmentSlot[] slots = equipmentSlots();
        int y = rightY + 34;
        int rowHeight = Math.min(40,
                Math.max(28, (panelHeight - 74) / slots.length));
        for (ScpEquipmentSlot slot : slots) {
            boolean selected = selection == Selection.EQUIPMENT
                    && selectedEquipment == slot;
            boolean hover = inside(mouseX, mouseY, rightX + 14, y,
                    rightWidth - 28, rowHeight - 4);
            graphics.fill(rightX + 14, y, rightX + rightWidth - 14,
                    y + rowHeight - 4,
                    selected ? ROW_SELECTED : hover ? ROW_HOVER : ROW);
            ItemStack stack = inventory.getEquipment(slot);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, rightX + 22, y + 6);
                graphics.renderItemDecorations(font, stack,
                        rightX + 22, y + 6);
            }
            graphics.drawString(font, roboto(slot.getDisplayName().toUpperCase()),
                    rightX + 48, y + 5,
                    selected ? 0xFF202020 : TEXT_BRIGHT, false);
            graphics.drawString(font,
                    roboto(stack.isEmpty() ? "None" : clipped(stack.getHoverName().getString(), rightWidth - 70)),
                    rightX + 48, y + 18,
                    selected ? 0xFF303030 : stack.isEmpty() ? TEXT_DIM : TEXT,
                    false);
            y += rowHeight;
        }
    }

    private void renderActions(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ActionButton> buttons = currentActions();
        if (buttons.isEmpty()) return;
        int gap = 6;
        int total = buttons.size() * 70 + (buttons.size() - 1) * gap;
        int x = leftX + Math.max(10, (leftWidth - total) / 2);
        int y = leftY + panelHeight - ACTION_HEIGHT - 8;
        for (ActionButton button : buttons) {
            boolean hover = inside(mouseX, mouseY, x, y, 70, ACTION_HEIGHT);
            graphics.fill(x, y, x + 70, y + ACTION_HEIGHT,
                    button.enabled ? hover ? BUTTON_HOVER : BUTTON
                            : BUTTON_DISABLED);
            graphics.drawCenteredString(font, roboto(button.label),
                    x + 35, y + 7,
                    button.enabled ? TEXT_BRIGHT : TEXT_DIM);
            button.x = x;
            button.y = y;
            x += 70 + gap;
        }
    }

    private void renderStatus(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSectionTitle(graphics, leftX, titleY, "STATUS");
        graphics.fill(leftX, leftY, rightX + rightWidth,
                leftY + panelHeight, PANEL);
        if (minecraft == null || minecraft.player == null) return;
        int x = leftX + 28;
        int y = leftY + 36;
        int width = rightX + rightWidth - leftX - 56;
        float health = minecraft.player.getHealth();
        float maxHealth = Math.max(1.0F, minecraft.player.getMaxHealth());
        drawStatusBar(graphics, x, y, width, "HEALTH",
                health / maxHealth, Math.round(health) + "/" + Math.round(maxHealth),
                HEALTH);
        drawStatusBar(graphics, x, y + 54, width, "STAMINA",
                PlayerVitalsClient.getStaminaRatio(),
                Math.round(PlayerVitalsClient.getStamina()) + "/"
                        + Math.round(PlayerVitalsClient.getMaxStamina()), STAMINA);
        graphics.drawString(font, montserrat("ACTIVE EQUIPMENT"), x,
                y + 122, TEXT_BRIGHT, false);
        int rowY = y + 143;
        if (inventory != null) {
            for (ScpEquipmentSlot slot : equipmentSlots()) {
                ItemStack stack = inventory.getEquipment(slot);
                graphics.drawString(font, roboto(slot.getDisplayName()), x,
                        rowY, TEXT_DIM, false);
                graphics.drawString(font,
                        roboto(stack.isEmpty() ? "None" : stack.getHoverName().getString()),
                        x + 92, rowY, stack.isEmpty() ? TEXT_DIM : TEXT, false);
                rowY += 17;
            }
        }
    }

    private void renderCodex(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSectionTitle(graphics, leftX, titleY, "CODEX");
        graphics.fill(leftX, leftY, rightX + rightWidth,
                leftY + panelHeight, PANEL);
        if (inventory == null || inventory.getDocuments().isEmpty()) {
            graphics.drawCenteredString(font, montserrat("No documents collected"),
                    (leftX + rightX + rightWidth) / 2,
                    leftY + panelHeight / 2, TEXT_DIM);
            return;
        }
        int x = leftX + 18;
        int y = leftY + 28;
        int width = rightX + rightWidth - leftX - 36;
        int visible = Math.max(1, (panelHeight - 42) / ROW_HEIGHT);
        clampScroll(inventory.getDocuments().size(), visible);
        int end = Math.min(inventory.getDocuments().size(), scroll + visible);
        for (int i = scroll; i < end; i++) {
            ItemStack stack = inventory.getDocuments().get(i);
            drawItemRow(graphics, x, y + (i - scroll) * ROW_HEIGHT,
                    width, stack, "Document",
                    selection == Selection.DOCUMENT && selectedIndex == i,
                    mouseX, mouseY);
        }
    }

    private void renderNavigation(GuiGraphics graphics, int mouseX, int mouseY) {
        int center = rootX + rootWidth / 2;
        int total = NAV_WIDTH * 3;
        int start = center - total / 2;
        drawNav(graphics, start, navY, "INVENTORY",
                mode == Mode.INVENTORY ? INVENTORY_ICON_SELECTED : INVENTORY_ICON,
                mode == Mode.INVENTORY, mouseX, mouseY);
        drawNav(graphics, start + NAV_WIDTH, navY, "STATUS",
                mode == Mode.STATUS ? STATUS_ICON_SELECTED : STATUS_ICON,
                mode == Mode.STATUS, mouseX, mouseY);
        drawNav(graphics, start + NAV_WIDTH * 2, navY, "CODEX",
                mode == Mode.CODEX ? CODEX_ICON_SELECTED : CODEX_ICON,
                mode == Mode.CODEX, mouseX, mouseY);
    }

    private void drawItemRow(GuiGraphics graphics, int x, int y, int width,
            ItemStack stack, String type, boolean selected,
            int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, width, ROW_HEIGHT - 4);
        graphics.fill(x, y, x + width, y + ROW_HEIGHT - 4,
                selected ? ROW_SELECTED : hover ? ROW_HOVER : ROW);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 7, y + 6);
            graphics.renderItemDecorations(font, stack, x + 7, y + 6);
        }
        int color = selected ? 0xFF202020 : TEXT_BRIGHT;
        graphics.drawString(font,
                roboto(clipped(stack.getHoverName().getString(), width - 42)),
                x + 31, y + 5, color, false);
        graphics.drawString(font, roboto(type), x + 31, y + 17,
                selected ? 0xFF303030 : TEXT_DIM, false);
    }

    private void drawStatusBar(GuiGraphics graphics, int x, int y,
            int width, String label, float ratio, String value, int color) {
        graphics.drawString(font, montserrat(label), x, y, TEXT_BRIGHT, false);
        graphics.drawString(font, roboto(value), x + width - font.width(value),
                y, TEXT, false);
        int barY = y + 17;
        graphics.fill(x, barY, x + width, barY + 17, PANEL_DARK);
        int fill = Math.round((width - 2) * Math.max(0.0F,
                Math.min(1.0F, ratio)));
        graphics.fill(x + 1, barY + 1, x + 1 + fill,
                barY + 16, color);
    }

    private void drawTab(GuiGraphics graphics, int x, int y, int width,
            int height, String label, boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, width, height);
        graphics.fill(x, y, x + width, y + height,
                active ? 0x887E898C : hover ? 0x665C6669 : 0x443A4244);
        graphics.drawCenteredString(font, roboto(label), x + width / 2,
                y + 5, active ? TEXT_BRIGHT : TEXT_DIM);
    }

    private void drawNav(GuiGraphics graphics, int x, int y, String label,
            ResourceLocation icon, boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, NAV_WIDTH, 48);
        if (hover && !active)
            graphics.fill(x + 4, y - 4, x + NAV_WIDTH - 4, y + 45,
                    0x334C5659);
        drawIcon(graphics, icon, x + (NAV_WIDTH - NAV_ICON_SIZE) / 2,
                y, NAV_ICON_SIZE);
        graphics.drawCenteredString(font, roboto(label), x + NAV_WIDTH / 2,
                y + NAV_ICON_SIZE + 7, active ? TEXT_BRIGHT : TEXT_DIM);
    }

    private void drawSectionTitle(GuiGraphics graphics, int x, int y,
            String title) {
        graphics.drawString(font, montserrat(title), x + 12, y,
                TEXT_BRIGHT, false);
    }

    private void drawEmpty(GuiGraphics graphics, int x, int y, int width,
            String text) {
        graphics.drawCenteredString(font, roboto(text), x + width / 2,
                y + 15, TEXT_DIM);
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, size, size, 0.0F, 0.0F,
                128, 128, 128, 128);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int center = rootX + rootWidth / 2;
        int start = center - NAV_WIDTH * 3 / 2;
        if (inside(mouseX, mouseY, start, navY, NAV_WIDTH, 48)) {
            switchMode(Mode.INVENTORY); return true;
        }
        if (inside(mouseX, mouseY, start + NAV_WIDTH, navY, NAV_WIDTH, 48)) {
            switchMode(Mode.STATUS); return true;
        }
        if (inside(mouseX, mouseY, start + NAV_WIDTH * 2, navY,
                NAV_WIDTH, 48)) {
            switchMode(Mode.CODEX); return true;
        }
        if (mode != Mode.INVENTORY || inventory == null)
            return super.mouseClicked(mouseX, mouseY, button);

        if (inside(mouseX, mouseY, inventoryTabX, tabY, tabWidth, 18)) {
            showingKeys = false; clearSelection(); scroll = 0; return true;
        }
        if (inside(mouseX, mouseY, keysTabX, tabY, tabWidth, 18)) {
            showingKeys = true; clearSelection(); scroll = 0; return true;
        }

        int listY = tabY + 27;
        int visible = Math.max(1, (panelHeight - (listY - leftY)
                - ACTION_HEIGHT - 22) / ROW_HEIGHT);
        int clickedRow = (int) ((mouseY - listY) / ROW_HEIGHT);
        if (mouseX >= leftX + 12 && mouseX < leftX + leftWidth - 12
                && clickedRow >= 0 && clickedRow < visible) {
            int logicalRow = scroll + clickedRow;
            if (showingKeys) {
                if (logicalRow < inventory.getKeys().size()) {
                    select(Selection.KEY, logicalRow, null);
                    return true;
                }
            } else {
                List<Integer> slots = nonEmptyMainSlots();
                if (logicalRow < slots.size()) {
                    int slot = slots.get(logicalRow);
                    boolean doubleClick = isDoubleClick(Selection.MAIN, slot);
                    select(Selection.MAIN, slot, null);
                    if (doubleClick) activateDefaultMainAction(slot);
                    return true;
                }
            }
        }

        int equipmentY = rightY + 34;
        int equipmentRowHeight = Math.min(40,
                Math.max(28, (panelHeight - 74) / equipmentSlots().length));
        int equipmentRow = (int) ((mouseY - equipmentY) / equipmentRowHeight);
        if (mouseX >= rightX + 14 && mouseX < rightX + rightWidth - 14
                && equipmentRow >= 0 && equipmentRow < equipmentSlots().length) {
            ScpEquipmentSlot slot = equipmentSlots()[equipmentRow];
            select(Selection.EQUIPMENT, -1, slot);
            return true;
        }

        for (ActionButton action : currentActions()) {
            if (action.enabled && inside(mouseX, mouseY, action.x, action.y,
                    70, ACTION_HEIGHT)) {
                sendAction(action.action);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mode == Mode.STATUS) return false;
        int size = mode == Mode.CODEX
                ? inventory == null ? 0 : inventory.getDocuments().size()
                : showingKeys
                ? inventory == null ? 0 : inventory.getKeys().size()
                : nonEmptyMainSlots().size();
        int visible = Math.max(1, (panelHeight - 80) / ROW_HEIGHT);
        int max = Math.max(0, size - visible);
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, max);
        return true;
    }

    private void switchMode(Mode next) {
        mode = next;
        scroll = 0;
        clearSelection();
    }

    private void select(Selection next, int index, ScpEquipmentSlot slot) {
        selection = next;
        selectedIndex = index;
        selectedEquipment = slot;
    }

    private void clearSelection() {
        selection = Selection.NONE;
        selectedIndex = -1;
        selectedEquipment = null;
    }

    private boolean isDoubleClick(Selection kind, int index) {
        long now = System.currentTimeMillis();
        boolean result = kind == lastClickSelection && index == lastClickIndex
                && now - lastClickAt <= DOUBLE_CLICK_MS;
        lastClickSelection = kind;
        lastClickIndex = index;
        lastClickAt = now;
        return result;
    }

    private void activateDefaultMainAction(int index) {
        ItemStack stack = inventory.getInventoryItem(index);
        ScpItemType type = ScpItemClassifier.getType(stack);
        if (type.isEquipment()) sendAction(Action.EQUIP_MAIN);
        else if (type == ScpItemType.CONSUMABLE) sendAction(Action.USE_MAIN);
    }

    private List<ActionButton> currentActions() {
        List<ActionButton> result = new ArrayList<>();
        if (inventory == null) return result;
        if (selection == Selection.MAIN && inventory.isValidMainSlot(selectedIndex)) {
            ItemStack stack = inventory.getInventoryItem(selectedIndex);
            if (stack.isEmpty()) return result;
            ScpItemType type = ScpItemClassifier.getType(stack);
            if (type == ScpItemType.CONSUMABLE)
                result.add(new ActionButton("USE", Action.USE_MAIN, true));
            if (type.isEquipment())
                result.add(new ActionButton("EQUIP", Action.EQUIP_MAIN, true));
            result.add(new ActionButton("DROP", Action.DROP_MAIN, true));
        } else if (selection == Selection.KEY
                && selectedIndex >= 0 && selectedIndex < inventory.getKeys().size()) {
            result.add(new ActionButton("DROP", Action.DROP_KEY, true));
        } else if (selection == Selection.EQUIPMENT
                && selectedEquipment != null
                && !inventory.getEquipment(selectedEquipment).isEmpty()) {
            result.add(new ActionButton("UNEQUIP", Action.UNEQUIP, true));
        }
        int gap = 6;
        int total = result.size() * 70 + Math.max(0, result.size() - 1) * gap;
        int x = leftX + Math.max(10, (leftWidth - total) / 2);
        int y = leftY + panelHeight - ACTION_HEIGHT - 8;
        for (ActionButton button : result) {
            button.x = x;
            button.y = y;
            x += 70 + gap;
        }
        return result;
    }

    private void sendAction(Action action) {
        ScpInventoryActionPacket packet = action == Action.UNEQUIP
                ? new ScpInventoryActionPacket(action, selectedEquipment)
                : new ScpInventoryActionPacket(action, selectedIndex);
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(packet);
    }

    private List<Integer> nonEmptyMainSlots() {
        List<Integer> result = new ArrayList<>();
        if (inventory == null) return result;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            if (!inventory.getInventoryItem(i).isEmpty()) result.add(i);
        }
        return result;
    }

    private void clampScroll(int size, int visible) {
        scroll = Mth.clamp(scroll, 0, Math.max(0, size - visible));
    }

    private static ScpEquipmentSlot[] equipmentSlots() {
        return new ScpEquipmentSlot[]{ScpEquipmentSlot.HEAD,
                ScpEquipmentSlot.CHEST, ScpEquipmentSlot.LEGS,
                ScpEquipmentSlot.FEET, ScpEquipmentSlot.ACCESSORY,
                ScpEquipmentSlot.WEAPON};
    }

    private String clipped(String text, int width) {
        return font.plainSubstrByWidth(text, Math.max(16, width));
    }

    private static Component roboto(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(ROBOTO));
    }

    private static Component montserrat(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(MONTSERRAT));
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ActionButton {
        private final String label;
        private final Action action;
        private final boolean enabled;
        private int x;
        private int y;

        private ActionButton(String label, Action action, boolean enabled) {
            this.label = label;
            this.action = action;
            this.enabled = enabled;
        }
    }
}
