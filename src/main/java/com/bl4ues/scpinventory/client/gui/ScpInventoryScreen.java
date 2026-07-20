package com.bl4ues.scpinventory.client.gui;

import com.bl4ues.scpinventory.client.ScpFonts;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.client.ClientInventoryBridge;
import com.bl4ues.scpinventory.client.gui.components.CodexPanel;
import com.bl4ues.scpinventory.client.gui.components.ContextMenu;
import com.bl4ues.scpinventory.client.gui.components.CraftingPanel;
import com.bl4ues.scpinventory.client.gui.components.EquipmentPanel;
import com.bl4ues.scpinventory.client.gui.components.ScrollableItemList;
import com.bl4ues.scpinventory.client.gui.components.StatusPanel;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

public class ScpInventoryScreen extends Screen {
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_SELECTED = 0xFF202020;
    private static final int TAB_ACTIVE = 0x55B2B3B3;
    private static final int TAB_INACTIVE = 0x336A6C6C;
    private static final int ROOT_TINT = 0x11000000;
    private static final int PANEL_BACKGROUND = 0x8F545D5F;
    private static final int FOOTER_BACKGROUND = 0x242B3133;
    private static final int DRAG_ICON_BOX = 0x99303638;
    private static final int DRAG_ICON_CORNER = 0xCC6A6C6C;
    private static final int EQUIPMENT_LINE_GRAY = 0x666A6C6C;
    private static final int EQUIPMENT_ICON_BOX = 0x66303638;
    private static final int EQUIPMENT_ICON_BORDER = 0xAA6A6C6C;
    private static final long DOUBLE_LEFT_CLICK_WINDOW_MS = 320L;
    private static final double DRAG_THRESHOLD = 4.0D;
    private static final float DROP_PREVIEW_UI_ALPHA = 0.25F;
    private static final float DROP_PREVIEW_SOLID_ITEM_ALPHA_THRESHOLD = 0.72F;

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/inventory_background.png");
    private static final ResourceLocation INVENTORY_ICON = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/inventoryicon.png");
    private static final ResourceLocation INVENTORY_ICON_SELECTED = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/inventoryicon_selected.png");
    private static final ResourceLocation STATUS_ICON = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/statusicon.png");
    private static final ResourceLocation STATUS_ICON_SELECTED = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/statusicon_selected.png");
    private static final ResourceLocation CRAFTING_ICON = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/craft.png");
    private static final ResourceLocation CRAFTING_ICON_SELECTED = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/craft_selected.png");
    private static final ResourceLocation CODEX_ICON = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/codexicon.png");
    private static final ResourceLocation CODEX_ICON_SELECTED = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/codexicon_selected.png");
    private static final ResourceLocation HEALTH_ICON = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "textures/gui/health.png");

    private static final int BACKGROUND_SOURCE_WIDTH = 1406;
    private static final int BACKGROUND_SOURCE_HEIGHT = 1080;
    private static final int SOURCE_ICON_SIZE = 128;
    private static final int NAV_ICON_SIZE = 24;
    private static final int NAV_BUTTON_WIDTH = 120;
    private static final int NAV_BUTTON_HEIGHT = 46;
    private static final int NAV_BUTTON_GAP = 24;
    private static final int TAB_HEIGHT = 17;
    private static final int INVENTORY_TAB_WIDTH = 104;
    private static final int KEYS_TAB_WIDTH = 104;
    private static final int HEALTH_ICON_SIZE = 20;
    private static final int DRAG_ICON_FRAME_SIZE = 24;
    private static final int EQUIPMENT_ROW_HEIGHT = 37;
    private static final int EQUIPMENT_ICON_BOX_SIZE = 24;
    private static final float HEALTH_TEXT_SCALE = 0.86F;

    private enum ScreenMode { INVENTORY, STATUS, CRAFTING, CODEX }
    private enum DragSourceKind { NONE, MAIN, EQUIPMENT }

    private static final ScpEquipmentSlot[] DROP_PREVIEW_EQUIPMENT_SLOTS = {
            ScpEquipmentSlot.HEAD,
            ScpEquipmentSlot.CHEST,
            ScpEquipmentSlot.LEGS,
            ScpEquipmentSlot.FEET,
            ScpEquipmentSlot.ACCESSORY,
            ScpEquipmentSlot.WEAPON
    };

    private ScrollableItemList itemList;
    private EquipmentPanel equipmentPanel;
    private CodexPanel codexPanel;
    private StatusPanel statusPanel;
    private CraftingPanel craftingPanel;
    private ContextMenu contextMenu;
    private IScpInventory inventory;
    private int contextIndex = -1;
    private boolean contextIsKey;
    private boolean showingKeys;
    private ScreenMode mode = ScreenMode.INVENTORY;

    private static ScreenMode rememberedMode = ScreenMode.INVENTORY;
    private static boolean rememberedShowingKeys = false;
    private static int rememberedInventoryScroll = 0;
    private static int rememberedKeysScroll = 0;
    private static int rememberedCodexSelection = -1;
    private static int rememberedCodexScroll = 0;
    private static int rememberedCodexTextScroll = 0;
    private static boolean rememberedCodexText = false;
    private static int rememberedStatusScroll = 0;
    private static boolean rememberedPositiveStatus = true;

    private int rootX, rootY, rootWidth, rootHeight;
    private int titleY, tabY, navY;
    private int listPanelX, listPanelY, listPanelWidth, listPanelHeight;
    private int equipmentPanelX, equipmentPanelY, equipmentPanelWidth, equipmentPanelHeight;
    private int listX, listY, listWidth;
    private int equipmentX, equipmentY, equipmentWidth;

    private int lastLeftClickIndex = -1;
    private boolean lastLeftClickWasKey = false;
    private long lastLeftClickTimeMs = 0L;
    private ScpEquipmentSlot lastEquipmentClickSlot = null;
    private long lastEquipmentClickTimeMs = 0L;

    private DragSourceKind dragSourceKind = DragSourceKind.NONE;
    private int dragSourceIndex = -1;
    private ScpEquipmentSlot dragSourceEquipmentSlot = null;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private double dragStartX = 0.0D;
    private double dragStartY = 0.0D;
    private boolean dragMoved = false;
    private boolean dropPreviewTransparentRender = false;
    private float dropPreviewFade = 0.0F;
    private float dropPreviewRenderAlpha = 1.0F;

    public ScpInventoryScreen() {
        super(Component.literal("SCP Inventory"));
    }

    @Override
    protected void init() {
        if (itemList != null || codexPanel != null || statusPanel != null
                || craftingPanel != null) {
            captureSessionState();
        }
        if (rememberUiState()) {
            mode = rememberedMode;
            showingKeys = rememberedShowingKeys;
        } else {
            resetSessionState();
            mode = ScreenMode.INVENTORY;
            showingKeys = false;
        }
        contextMenu = new ContextMenu();
        computeLayout();

        craftingPanel = new CraftingPanel(
                listPanelX + 10,
                listPanelY,
                listPanelWidth - 20,
                listPanelHeight,
                equipmentPanelX + 10,
                equipmentPanelY,
                equipmentPanelWidth - 20,
                equipmentPanelHeight,
                titleY,
                listPanelX,
                equipmentPanelX
        );

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ScpInventoryCapability.get(mc.player).ifPresent(inv -> {
            inventory = inv;
            rebuildItemList();
            equipmentPanel = new EquipmentPanel(equipmentX, equipmentY, equipmentWidth, titleY, equipmentPanelX, inv);

            int codexY = listPanelY + 26;
            int codexPanelHeight = Math.max(120, listPanelHeight - 26);
            codexPanel = new CodexPanel(
                    listPanelX + 10,
                    codexY,
                    listPanelWidth - 20,
                    codexPanelHeight,
                    equipmentPanelX + 10,
                    equipmentPanelWidth - 20,
                    codexPanelHeight,
                    titleY,
                    listPanelX,
                    equipmentPanelX,
                    inv.getDocuments()
            );
            if (rememberUiState()) {
                codexPanel.restoreSessionState(rememberedCodexSelection, rememberedCodexScroll,
                        rememberedCodexTextScroll, rememberedCodexText);
            }

            int statusY = listPanelY;
            int statusPanelHeight = listPanelHeight;
            statusPanel = new StatusPanel(
                    listPanelX + 10,
                    statusY,
                    listPanelWidth - 20,
                    statusPanelHeight,
                    equipmentPanelX + 10,
                    statusY,
                    equipmentPanelWidth - 20,
                    statusPanelHeight,
                    titleY,
                    listPanelX,
                    equipmentPanelX
            );
            if (rememberUiState()) {
                statusPanel.restoreSessionState(rememberedStatusScroll, rememberedPositiveStatus);
            }
        });
    }

    private void computeLayout() {
        int margin = 24;
        int availableWidth = width - (margin * 2);
        int availableHeight = height - (margin * 2);
        float aspect = BACKGROUND_SOURCE_WIDTH / (float) BACKGROUND_SOURCE_HEIGHT;

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

        int sideMargin = Math.round(rootWidth * 0.055F);
        int panelGap = Math.round(rootWidth * 0.040F);
        int sharedPanelWidth = (rootWidth - (sideMargin * 2) - panelGap) / 2;

        listPanelX = rootX + sideMargin;
        equipmentPanelX = listPanelX + sharedPanelWidth + panelGap;
        listPanelWidth = sharedPanelWidth;
        equipmentPanelWidth = sharedPanelWidth;

        listPanelY = tabY - 5;
        equipmentPanelY = listPanelY;
        int panelBottom = navY - Math.round(rootHeight * 0.035F);
        listPanelHeight = Math.max(300, panelBottom - listPanelY);
        equipmentPanelHeight = listPanelHeight;

        listX = listPanelX + 18;
        listY = tabY + 31;
        listWidth = listPanelWidth - 36;

        equipmentX = equipmentPanelX + 28;
        equipmentY = equipmentPanelY + 56;
        equipmentWidth = equipmentPanelWidth - 56;
    }

    private void rebuildItemList() {
        if (inventory == null) return;
        itemList = showingKeys
                ? new ScrollableItemList(listX, listY, listWidth, inventory.getKeys(), inventory, "Key")
                : new ScrollableItemList(listX, listY, listWidth, inventory.getInventory(), inventory);
        if (rememberUiState()) itemList.setScrollOffset(showingKeys ? rememberedKeysScroll : rememberedInventoryScroll);
    }

    @Override
    public void tick() {
        super.tick();
        if (!InventoryModuleRuntimeState.isEnabledForClient()) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean worldDropPreview = isPreviewingWorldDrop(mouseX, mouseY);
        updateDropPreviewFade(worldDropPreview);

        if (dropPreviewFade > 0.01F) {
            renderTransparentDropPreview(g, mouseX, mouseY);
            return;
        }

        renderBackground(g, mouseX, mouseY, partialTick);
        renderPanels(g);
        renderHealthStatus(g);

        boolean codexExpanded = mode == ScreenMode.CODEX && codexPanel != null && codexPanel.isExpandedImage();

        if (mode == ScreenMode.CODEX) {
            if (codexPanel != null) codexPanel.render(g, mouseX, mouseY);
        } else if (mode == ScreenMode.STATUS) {
            if (statusPanel != null) statusPanel.render(g, mouseX, mouseY);
        } else if (mode == ScreenMode.CRAFTING) {
            if (craftingPanel != null) craftingPanel.render(g, mouseX, mouseY);
        } else {
            renderInventoryHeader(g);
            renderTabs(g);
            if (itemList != null) itemList.render(g, mouseX, mouseY);
            if (equipmentPanel != null) equipmentPanel.render(g, mouseX, mouseY);
        }

        if (!codexExpanded) {
            renderBottomNavigation(g);
            if (contextMenu != null) contextMenu.render(g, mouseX, mouseY);
            renderDraggedStack(g, mouseX, mouseY);
            super.render(g, mouseX, mouseY, partialTick);
        }
    }

    private void updateDropPreviewFade(boolean targetVisible) {
        float target = targetVisible ? 1.0F : 0.0F;
        float speed = targetVisible ? 0.055F : 0.070F;
        dropPreviewFade += (target - dropPreviewFade) * speed;
        if (Math.abs(dropPreviewFade - target) < 0.008F) dropPreviewFade = target;
    }

    private float getDropPreviewUiAlpha() {
        return 1.0F - dropPreviewFade * (1.0F - DROP_PREVIEW_UI_ALPHA);
    }

    private void renderTransparentDropPreview(GuiGraphics g, int mouseX, int mouseY) {
        renderPreviewBackgroundDim(g);
        dropPreviewTransparentRender = true;
        dropPreviewRenderAlpha = getDropPreviewUiAlpha();
        g.setColor(1.0F, 1.0F, 1.0F, dropPreviewRenderAlpha);

        renderPanels(g);
        renderHealthStatus(g);
        if (mode == ScreenMode.CODEX) {
            if (codexPanel != null) codexPanel.render(g, mouseX, mouseY);
        } else if (mode == ScreenMode.STATUS) {
            if (statusPanel != null) statusPanel.render(g, mouseX, mouseY);
        } else if (mode == ScreenMode.CRAFTING) {
            if (craftingPanel != null) craftingPanel.render(g, mouseX, mouseY);
        } else {
            renderInventoryHeader(g);
            renderTabs(g);
            if (itemList != null) itemList.render(g, mouseX, mouseY, dropPreviewRenderAlpha);
            renderDropPreviewEquipmentPanel(g);
        }
        renderBottomNavigation(g);

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        dropPreviewTransparentRender = false;
        dropPreviewRenderAlpha = 1.0F;
        renderDraggedStack(g, mouseX, mouseY);
    }

    private void renderPreviewBackgroundDim(GuiGraphics g) {
        float dimAlpha = 0.35F * (1.0F - dropPreviewFade);
        if (dimAlpha <= 0.01F) return;
        int alpha = Math.max(1, Math.min(255, Math.round(255.0F * dimAlpha)));
        g.fill(0, 0, width, height, alpha << 24);
    }

    private void renderPanels(GuiGraphics g) {
        blitSmoothTexture(g, BACKGROUND, rootX, rootY, rootWidth, rootHeight, BACKGROUND_SOURCE_WIDTH, BACKGROUND_SOURCE_HEIGHT);
        g.fill(rootX, rootY, rootX + rootWidth, rootY + rootHeight, uiColor(ROOT_TINT));
        g.fill(rootX, navY - 18, rootX + rootWidth, rootY + rootHeight, uiColor(FOOTER_BACKGROUND));
        int panelBottom = listPanelY + listPanelHeight;
        g.fill(listPanelX, listPanelY, listPanelX + listPanelWidth, panelBottom, uiColor(PANEL_BACKGROUND));
        g.fill(equipmentPanelX, equipmentPanelY, equipmentPanelX + equipmentPanelWidth, equipmentPanelY + equipmentPanelHeight, uiColor(PANEL_BACKGROUND));
    }

    private void renderHealthStatus(GuiGraphics g) {
        if (minecraft == null || minecraft.player == null) return;
        int healthX = rootX + Math.round(rootWidth * 0.038F);
        int healthY = rootY + Math.round(rootHeight * 0.032F);
        int textX = healthX + HEALTH_ICON_SIZE + 7;
        blitFullIcon(g, HEALTH_ICON, healthX, healthY - 1, HEALTH_ICON_SIZE, HEALTH_ICON_SIZE);

        int health = Math.round(minecraft.player.getHealth());
        int maxHealth = Math.round(minecraft.player.getMaxHealth());
        int percent = maxHealth <= 0 ? 0 : Math.round((health / (float) maxHealth) * 100.0F);
        drawScaledString(g, "HEALTH", textX, healthY, TEXT_WHITE, HEALTH_TEXT_SCALE);
        drawScaledString(g, percent + "/100", textX, healthY + 13, TEXT_WHITE, HEALTH_TEXT_SCALE);
    }

    private void renderInventoryHeader(GuiGraphics g) {
        drawSectionTitle(g, listPanelX, titleY, "BACKPACK");
        if (inventory == null) {
            drawRightAlignedCount(g, 0, " of 12 items");
            return;
        }
        if (showingKeys) drawRightAlignedCount(g, inventory.getKeyCount(), " of 12 keys");
        else drawRightAlignedCount(g, inventory.getInventoryCount(), " of " + inventory.getMaxMainSlots() + " items");
    }

    private void drawRightAlignedCount(GuiGraphics g, int current, String suffix) {
        String currentText = Integer.toString(current);
        int totalWidth = minecraft.font.width(ScpFonts.roboto(currentText)) + minecraft.font.width(ScpFonts.roboto(suffix));
        int x = listX + listWidth - totalWidth;
        g.drawString(minecraft.font, ScpFonts.roboto(currentText), x, titleY, uiColor(TEXT_WHITE), false);
        g.drawString(minecraft.font, ScpFonts.roboto(suffix), x + minecraft.font.width(ScpFonts.roboto(currentText)), titleY, uiColor(TEXT_GRAY), false);
    }

    private void renderTabs(GuiGraphics g) {
        int tabWidth = INVENTORY_TAB_WIDTH;
        int tabDrawY = tabY + 3;
        int gap = 12;
        int total = (tabWidth * 2) + gap;
        int startX = listPanelX + Math.max(0, (listPanelWidth - total) / 2);
        drawTab(g, startX, tabDrawY, tabWidth, "INVENTORY", !showingKeys);
        drawTab(g, startX + tabWidth + gap, tabDrawY, tabWidth, "KEYS", showingKeys);
    }

    private void renderDropPreviewEquipmentPanel(GuiGraphics g) {
        if (inventory == null) return;
        drawSectionTitle(g, equipmentPanelX, titleY, "EQUIPMENT");
        int rowY = equipmentY;
        for (ScpEquipmentSlot slot : DROP_PREVIEW_EQUIPMENT_SLOTS) {
            renderDropPreviewEquipmentSlot(g, slot, rowY);
            rowY += EQUIPMENT_ROW_HEIGHT;
        }
    }

    private void renderDropPreviewEquipmentSlot(GuiGraphics g, ScpEquipmentSlot slot, int rowY) {
        ItemStack stack = inventory.getEquipment(slot);
        int iconX = equipmentX + 8;
        int iconY = rowY + 6;
        int textX = equipmentX + 44;
        if (stack.isEmpty()) {
            drawDropPreviewEmptyCorners(g, iconX, iconY);
        } else {
            drawDropPreviewFilledFrame(g, iconX, iconY);
            if (dropPreviewRenderAlpha >= DROP_PREVIEW_SOLID_ITEM_ALPHA_THRESHOLD) g.renderItem(stack, iconX + 4, iconY + 4);
        }
        Component itemName = stack.isEmpty() ? Component.literal("None") : stack.getHoverName();
        g.drawString(minecraft.font, ScpFonts.roboto(slot.getDisplayName()), textX, rowY + 7, uiColor(TEXT_WHITE), false);
        g.drawString(minecraft.font, ScpFonts.roboto(itemName), textX, rowY + 20, uiColor(stack.isEmpty() ? TEXT_GRAY : TEXT_WHITE), false);
        int lineY = rowY + EQUIPMENT_ROW_HEIGHT - 1;
        g.fill(equipmentX, lineY, equipmentX + equipmentWidth, lineY + 1, uiColor(EQUIPMENT_LINE_GRAY));
    }

    private void drawDropPreviewFilledFrame(GuiGraphics g, int x, int y) {
        int right = x + EQUIPMENT_ICON_BOX_SIZE;
        int bottom = y + EQUIPMENT_ICON_BOX_SIZE;
        g.fill(x, y, right, bottom, uiColor(EQUIPMENT_ICON_BOX));
        g.fill(x, y, right, y + 1, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(x, bottom - 1, right, bottom, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(x, y, x + 1, bottom, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(right - 1, y, right, bottom, uiColor(EQUIPMENT_ICON_BORDER));
    }

    private void drawDropPreviewEmptyCorners(GuiGraphics g, int x, int y) {
        int right = x + EQUIPMENT_ICON_BOX_SIZE;
        int bottom = y + EQUIPMENT_ICON_BOX_SIZE;
        int corner = 6;
        g.fill(x, y, x + corner, y + 1, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(x, y, x + 1, y + corner, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(right - corner, y, right, y + 1, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(right - 1, y, right, y + corner, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(x, bottom - 1, x + corner, bottom, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(x, bottom - corner, x + 1, bottom, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(right - corner, bottom - 1, right, bottom, uiColor(EQUIPMENT_ICON_BORDER));
        g.fill(right - 1, bottom - corner, right, bottom, uiColor(EQUIPMENT_ICON_BORDER));
    }

    private void renderBottomNavigation(GuiGraphics g) {
        drawNavigationButton(g, getInventoryNavX(), navY, "INVENTORY", mode == ScreenMode.INVENTORY ? INVENTORY_ICON_SELECTED : INVENTORY_ICON, mode == ScreenMode.INVENTORY);
        drawNavigationButton(g, getStatusNavX(), navY, "STATUS", mode == ScreenMode.STATUS ? STATUS_ICON_SELECTED : STATUS_ICON, mode == ScreenMode.STATUS);
        drawNavigationButton(g, getCraftingNavX(), navY, "CRAFTING", mode == ScreenMode.CRAFTING ? CRAFTING_ICON_SELECTED : CRAFTING_ICON, mode == ScreenMode.CRAFTING);
        drawNavigationButton(g, getCodexNavX(), navY, "CODEX", mode == ScreenMode.CODEX ? CODEX_ICON_SELECTED : CODEX_ICON, mode == ScreenMode.CODEX);
    }

    private void drawNavigationButton(GuiGraphics g, int x, int y, String label, ResourceLocation icon, boolean active) {
        int iconX = x + (NAV_BUTTON_WIDTH - NAV_ICON_SIZE) / 2;
        int textX = x + (NAV_BUTTON_WIDTH - minecraft.font.width(ScpFonts.roboto(label))) / 2;
        blitFullIcon(g, icon, iconX, y, NAV_ICON_SIZE, NAV_ICON_SIZE);
        g.drawString(minecraft.font, ScpFonts.roboto(label), textX, y + NAV_ICON_SIZE + 6, uiColor(active ? TEXT_WHITE : TEXT_GRAY), false);
    }

    private void blitFullIcon(GuiGraphics g, ResourceLocation icon, int x, int y, int width, int height) {
        blitSmoothTexture(g, icon, x, y, width, height, SOURCE_ICON_SIZE, SOURCE_ICON_SIZE);
    }

    private void blitSmoothTexture(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        setTextureFiltering(texture, true);
        float alpha = dropPreviewTransparentRender ? dropPreviewRenderAlpha : 1.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(texture, x, y, width, height, 0.0F, 0.0F, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        setTextureFiltering(texture, false);
    }

    private void setTextureFiltering(ResourceLocation texture, boolean blur) {
        if (minecraft == null) return;
        minecraft.getTextureManager().getTexture(texture).setFilter(blur, false);
    }

    private void drawScaledString(GuiGraphics g, String text, float x, float y, int color, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(minecraft.font, ScpFonts.roboto(text), 0, 0, uiColor(color), false);
        g.pose().popPose();
    }

    private void drawTab(GuiGraphics g, int x, int y, int w, String label, boolean active) {
        g.fill(x, y, x + w, y + TAB_HEIGHT, uiColor(active ? TAB_ACTIVE : TAB_INACTIVE));
        g.drawString(minecraft.font, ScpFonts.roboto(label), x + (w - minecraft.font.width(ScpFonts.roboto(label))) / 2, y + 5, uiColor(active ? TEXT_SELECTED : TEXT_WHITE), false);
    }

    private void drawSectionTitle(GuiGraphics g, int x, int y, String suffix) {
        String prefix = "://INVENTORY_";
        g.drawString(minecraft.font, ScpFonts.roboto(prefix), x, y, uiColor(TEXT_GRAY), false);
        g.drawString(minecraft.font, ScpFonts.roboto(suffix), x + minecraft.font.width(ScpFonts.roboto(prefix)), y, uiColor(TEXT_WHITE), false);
    }

    private int uiColor(int color) {
        if (!dropPreviewTransparentRender) return color;
        int alpha = color >>> 24;
        int fadedAlpha = Math.max(1, Math.round(alpha * dropPreviewRenderAlpha));
        return (fadedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private void renderDraggedStack(GuiGraphics g, int mouseX, int mouseY) {
        if (draggedStack.isEmpty() || !dragMoved) return;
        int frameX = mouseX - (DRAG_ICON_FRAME_SIZE / 2);
        int frameY = mouseY - (DRAG_ICON_FRAME_SIZE / 2);
        drawDragIconFrame(g, frameX, frameY);
        g.renderItem(draggedStack, frameX + 4, frameY + 4);
    }

    private void drawDragIconFrame(GuiGraphics g, int x, int y) {
        int right = x + DRAG_ICON_FRAME_SIZE;
        int bottom = y + DRAG_ICON_FRAME_SIZE;
        int corner = 6;
        g.fill(x, y, right, bottom, DRAG_ICON_BOX);
        g.fill(x, y, x + corner, y + 1, DRAG_ICON_CORNER);
        g.fill(x, y, x + 1, y + corner, DRAG_ICON_CORNER);
        g.fill(right - corner, y, right, y + 1, DRAG_ICON_CORNER);
        g.fill(right - 1, y, right, y + corner, DRAG_ICON_CORNER);
        g.fill(x, bottom - 1, x + corner, bottom, DRAG_ICON_CORNER);
        g.fill(x, bottom - corner, x + 1, bottom, DRAG_ICON_CORNER);
        g.fill(right - corner, bottom - 1, right, bottom, DRAG_ICON_CORNER);
        g.fill(right - 1, bottom - corner, right, bottom, DRAG_ICON_CORNER);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (mode == ScreenMode.STATUS && statusPanel != null && statusPanel.mouseScrolled(mouseX, mouseY, scrollY)) return true;
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.isMouseOver(mouseX, mouseY)) return itemList.mouseScrolled(scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.isExpandedImage()) return codexPanel.mouseClicked(mouseX, mouseY, button);
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseClickedScrollbar(mouseX, mouseY, button)) return true;
        if (mode == ScreenMode.STATUS && statusPanel != null && statusPanel.mouseClickedScrollbar(mouseX, mouseY, button)) return true;
        if (button == 0 && clickedBottomNavigation(mouseX, mouseY)) return true;
        if (mode == ScreenMode.STATUS || mode == ScreenMode.CRAFTING) return super.mouseClicked(mouseX, mouseY, button);
        if (mode == ScreenMode.CODEX) return codexPanel != null && codexPanel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && clickedTabs(mouseX, mouseY)) return true;

        if (button == 0 && contextMenu != null && contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option != -1) {
                handleAction(contextMenu.getOption(option));
                contextMenu.close();
                return true;
            }
            contextMenu.close();
        }

        if (inventory == null) return false;
        if (equipmentPanel != null) {
            ScpEquipmentSlot clickedEquipmentSlot = equipmentPanel.getClickedSlot(mouseX, mouseY);
            if (clickedEquipmentSlot != null && !inventory.getEquipment(clickedEquipmentSlot).isEmpty()) {
                if (button == 0) {
                    if (isDoubleEquipmentClick(clickedEquipmentSlot)) {
                        resetEquipmentClickMemory();
                        resetLeftClickMemory();
                        clearDragSource();
                        ClientInventoryBridge.moveEquipmentToMain(clickedEquipmentSlot, -1);
                        return true;
                    }
                    startEquipmentDrag(clickedEquipmentSlot, mouseX, mouseY);
                    return true;
                }
                if (button == 1) return true;
            }
        }

        if (itemList == null) return false;
        return showingKeys ? handleKeyClick(mouseX, mouseY, button) : handleMainInventoryClick(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mode == ScreenMode.CRAFTING && craftingPanel != null
                && craftingPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (mode == ScreenMode.STATUS && statusPanel != null && statusPanel.mouseDraggedScrollbar(mouseY)) return true;
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseDraggedScrollbar(mouseY)) return true;
        if (button == 0 && hasDragSource()) {
            if (Math.abs(mouseX - dragStartX) > DRAG_THRESHOLD || Math.abs(mouseY - dragStartY) > DRAG_THRESHOLD) dragMoved = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.mouseReleased(button)) return true;
        if (mode == ScreenMode.STATUS && statusPanel != null && statusPanel.mouseReleasedScrollbar(button)) return true;
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.mouseReleasedScrollbar(button)) return true;
        if (button == 0 && hasDragSource()) {
            if (dragMoved) finishDrag(mouseX, mouseY);
            clearDragSource();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handleMainInventoryClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (!inventory.isValidMainSlot(index)) return false;
        ItemStack stack = inventory.getInventoryItem(index);
        if (stack.isEmpty()) return false;
        if (button == 0 && itemList.clickedDrop(mouseX)) {
            ClientInventoryBridge.perform(index, InventoryActionPacket.ACTION_DROP);
            return true;
        }
        if (button == 0) {
            if (isDoubleLeftClick(index, false) && performDefaultItemAction(index, stack)) {
                resetLeftClickMemory();
                if (contextMenu != null) contextMenu.close();
                return true;
            }
            startMainDrag(index, stack, mouseX, mouseY);
            return true;
        }
        if (button == 1) {
            contextIndex = index;
            contextIsKey = false;
            contextMenu.open((int) mouseX, (int) mouseY, ScpItemClassifier.getDisplayType(stack));
            return true;
        }
        return false;
    }

    private boolean handleKeyClick(double mouseX, double mouseY, int button) {
        int index = itemList.getClickedIndex(mouseX, mouseY);
        if (index < 0 || index >= inventory.getKeys().size()) return false;
        ItemStack key = inventory.getKeys().get(index);
        if (key.isEmpty()) return false;
        if (button == 1) {
            contextIndex = index;
            contextIsKey = true;
            contextMenu.open((int) mouseX, (int) mouseY, "Key");
            return true;
        }
        return false;
    }

    private boolean performDefaultItemAction(int index, ItemStack stack) {
        if (ScpItemClassifier.getEquipmentSlot(stack).isPresent()) {
            ClientInventoryBridge.perform(index, InventoryActionPacket.ACTION_EQUIP);
            return true;
        }
        String itemType = inventory.getItemType(index);
        if ("Consumable".equals(itemType) || "Usable".equals(itemType)) {
            ClientInventoryBridge.perform(index, InventoryActionPacket.ACTION_USE);
            return true;
        }
        return false;
    }

    private void startMainDrag(int index, ItemStack stack, double mouseX, double mouseY) {
        if (contextMenu != null) contextMenu.close();
        dragSourceKind = DragSourceKind.MAIN;
        dragSourceIndex = index;
        dragSourceEquipmentSlot = null;
        draggedStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragMoved = false;
    }

    private void startEquipmentDrag(ScpEquipmentSlot slot, double mouseX, double mouseY) {
        if (slot == null || inventory == null) return;
        ItemStack stack = inventory.getEquipment(slot);
        if (stack.isEmpty()) return;
        if (contextMenu != null) contextMenu.close();
        dragSourceKind = DragSourceKind.EQUIPMENT;
        dragSourceIndex = -1;
        dragSourceEquipmentSlot = slot;
        draggedStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragMoved = false;
    }

    private boolean hasDragSource() {
        return dragSourceKind != DragSourceKind.NONE && !draggedStack.isEmpty();
    }

    private boolean isPreviewingWorldDrop(double mouseX, double mouseY) {
        return hasDragSource() && dragMoved && !isInsideRoot(mouseX, mouseY);
    }

    private void clearDragSource() {
        dragSourceKind = DragSourceKind.NONE;
        dragSourceIndex = -1;
        dragSourceEquipmentSlot = null;
        draggedStack = ItemStack.EMPTY;
        dragStartX = 0.0D;
        dragStartY = 0.0D;
        dragMoved = false;
    }

    private void finishDrag(double mouseX, double mouseY) {
        if (!isInsideRoot(mouseX, mouseY)) {
            dropDragSourceToWorld();
            return;
        }
        ScpEquipmentSlot targetEquipmentSlot = equipmentPanel == null ? null : equipmentPanel.getClickedSlot(mouseX, mouseY);
        if (targetEquipmentSlot != null) {
            dropDragSourceToEquipment(targetEquipmentSlot);
            return;
        }
        if (itemList != null && isInsideListPanel(mouseX, mouseY)) {
            int targetIndex = itemList.getClickedIndex(mouseX, mouseY);
            dropDragSourceToMain(targetIndex);
        }
    }

    private void dropDragSourceToWorld() {
        if (dragSourceKind == DragSourceKind.MAIN) ClientInventoryBridge.moveMainToWorld(dragSourceIndex);
        else if (dragSourceKind == DragSourceKind.EQUIPMENT) ClientInventoryBridge.moveEquipmentToWorld(dragSourceEquipmentSlot);
    }

    private void dropDragSourceToEquipment(ScpEquipmentSlot targetEquipmentSlot) {
        if (dragSourceKind == DragSourceKind.MAIN) ClientInventoryBridge.moveMainToEquipment(dragSourceIndex, targetEquipmentSlot);
        else if (dragSourceKind == DragSourceKind.EQUIPMENT) ClientInventoryBridge.moveEquipmentToEquipment(dragSourceEquipmentSlot, targetEquipmentSlot);
    }

    private void dropDragSourceToMain(int targetIndex) {
        if (dragSourceKind == DragSourceKind.MAIN) {
            if (inventory != null && inventory.isValidMainSlot(targetIndex) && targetIndex != dragSourceIndex) ClientInventoryBridge.moveMainToMain(dragSourceIndex, targetIndex);
        } else if (dragSourceKind == DragSourceKind.EQUIPMENT) {
            ClientInventoryBridge.moveEquipmentToMain(dragSourceEquipmentSlot, targetIndex);
        }
    }

    private boolean isInsideRoot(double mouseX, double mouseY) {
        return mouseX >= rootX && mouseX <= rootX + rootWidth && mouseY >= rootY && mouseY <= rootY + rootHeight;
    }

    private boolean isInsideListPanel(double mouseX, double mouseY) {
        return mouseX >= listPanelX && mouseX <= listPanelX + listPanelWidth && mouseY >= listPanelY && mouseY <= listPanelY + listPanelHeight;
    }

    private boolean isDoubleLeftClick(int index, boolean keyList) {
        long now = System.currentTimeMillis();
        boolean result = index == lastLeftClickIndex && keyList == lastLeftClickWasKey && now - lastLeftClickTimeMs <= DOUBLE_LEFT_CLICK_WINDOW_MS;
        lastLeftClickIndex = index;
        lastLeftClickWasKey = keyList;
        lastLeftClickTimeMs = now;
        return result;
    }

    private boolean isDoubleEquipmentClick(ScpEquipmentSlot slot) {
        long now = System.currentTimeMillis();
        boolean result = slot == lastEquipmentClickSlot && now - lastEquipmentClickTimeMs <= DOUBLE_LEFT_CLICK_WINDOW_MS;
        lastEquipmentClickSlot = slot;
        lastEquipmentClickTimeMs = now;
        return result;
    }

    private void resetLeftClickMemory() {
        lastLeftClickIndex = -1;
        lastLeftClickWasKey = false;
        lastLeftClickTimeMs = 0L;
    }

    private void resetEquipmentClickMemory() {
        lastEquipmentClickSlot = null;
        lastEquipmentClickTimeMs = 0L;
    }

    private boolean clickedTabs(double mouseX, double mouseY) {
        int tabDrawY = tabY + 3;
        if (mouseY < tabDrawY || mouseY > tabDrawY + TAB_HEIGHT) return false;
        int tabWidth = INVENTORY_TAB_WIDTH;
        int gap = 12;
        int total = (tabWidth * 2) + gap;
        int startX = listPanelX + Math.max(0, (listPanelWidth - total) / 2);
        if (mouseX >= startX && mouseX <= startX + tabWidth) {
            rememberCurrentItemScroll();
            showingKeys = false;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }
        int keysX = startX + tabWidth + gap;
        if (mouseX >= keysX && mouseX <= keysX + tabWidth) {
            rememberCurrentItemScroll();
            showingKeys = true;
            contextIsKey = false;
            rebuildItemList();
            return true;
        }
        return false;
    }

    private boolean clickedBottomNavigation(double mouseX, double mouseY) {
        if (mouseY < navY || mouseY > navY + NAV_BUTTON_HEIGHT) return false;
        if (mouseX >= getInventoryNavX() && mouseX <= getInventoryNavX() + NAV_BUTTON_WIDTH) {
            setMode(ScreenMode.INVENTORY);
            return true;
        }
        if (mouseX >= getStatusNavX() && mouseX <= getStatusNavX() + NAV_BUTTON_WIDTH) {
            setMode(ScreenMode.STATUS);
            return true;
        }
        if (mouseX >= getCraftingNavX() && mouseX <= getCraftingNavX() + NAV_BUTTON_WIDTH) {
            setMode(ScreenMode.CRAFTING);
            return true;
        }
        if (mouseX >= getCodexNavX() && mouseX <= getCodexNavX() + NAV_BUTTON_WIDTH) {
            setMode(ScreenMode.CODEX);
            return true;
        }
        return false;
    }

    private void setMode(ScreenMode newMode) {
        if (mode == ScreenMode.INVENTORY) rememberCurrentItemScroll();
        mode = newMode;
        if (contextMenu != null) contextMenu.close();
        if (newMode != ScreenMode.INVENTORY) clearDragSource();
    }

    private int getNavigationStartX() {
        int totalWidth = (NAV_BUTTON_WIDTH * 4) + (NAV_BUTTON_GAP * 3);
        return rootX + (rootWidth - totalWidth) / 2;
    }

    private int getInventoryNavX() {
        return getNavigationStartX();
    }

    private int getStatusNavX() {
        return getNavigationStartX() + NAV_BUTTON_WIDTH + NAV_BUTTON_GAP;
    }

    private int getCraftingNavX() {
        return getNavigationStartX() + (NAV_BUTTON_WIDTH + NAV_BUTTON_GAP) * 2;
    }

    private int getCodexNavX() {
        return getNavigationStartX() + (NAV_BUTTON_WIDTH + NAV_BUTTON_GAP) * 3;
    }

    private void handleAction(String action) {
        if (contextIndex < 0) return;
        if (contextIsKey) ClientInventoryBridge.performKey(contextIndex, action);
        else ClientInventoryBridge.perform(contextIndex, action);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        captureSessionState();
        super.removed();
    }

    private static boolean rememberUiState() {
        return ScpAdditionsModulesConfig.get().inventory.enabled
                && ScpAdditionsModulesConfig.get().inventory.rememberUiState;
    }

    private void rememberCurrentItemScroll() {
        if (!rememberUiState() || itemList == null) return;
        if (showingKeys) rememberedKeysScroll = itemList.getScrollOffset();
        else rememberedInventoryScroll = itemList.getScrollOffset();
    }

    private void captureSessionState() {
        if (!rememberUiState()) return;
        rememberedMode = mode;
        rememberedShowingKeys = showingKeys;
        rememberCurrentItemScroll();
        if (codexPanel != null) {
            rememberedCodexSelection = codexPanel.getSelectedIndex();
            rememberedCodexScroll = codexPanel.getScrollOffset();
            rememberedCodexTextScroll = codexPanel.getTextScrollOffset();
            rememberedCodexText = codexPanel.isShowingText();
        }
        if (statusPanel != null) {
            rememberedStatusScroll = statusPanel.getConditionsScroll();
            rememberedPositiveStatus = statusPanel.isShowingPositiveConditions();
        }
    }

    public static void resetSessionState() {
        rememberedMode = ScreenMode.INVENTORY;
        rememberedShowingKeys = false;
        rememberedInventoryScroll = 0;
        rememberedKeysScroll = 0;
        rememberedCodexSelection = -1;
        rememberedCodexScroll = 0;
        rememberedCodexTextScroll = 0;
        rememberedCodexText = false;
        rememberedStatusScroll = 0;
        rememberedPositiveStatus = true;
    }
}
