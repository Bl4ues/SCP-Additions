package com.bl4ues.scpclassifieddirective.inventory.client.gui;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.inventory.client.ClientInventoryBridge;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.CodexPanel;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.ContextMenu;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.CraftingPanel;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.EquipmentPanel;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.ScrollableItemList;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.StatusPanel;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpEquipmentSlot;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.network.InventoryActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.InventoryPdaStatePacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.client.pda.InventoryPdaPresentationRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;

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
    private static final long DOUBLE_LEFT_CLICK_WINDOW_MS = 320L;
    private static final double DRAG_THRESHOLD = 4.0D;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventory_background.png");
    private static final ResourceLocation INVENTORY_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon.png");
    private static final ResourceLocation INVENTORY_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/inventoryicon_selected.png");
    private static final ResourceLocation STATUS_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/statusicon.png");
    private static final ResourceLocation STATUS_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/statusicon_selected.png");
    private static final ResourceLocation CRAFTING_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/craft.png");
    private static final ResourceLocation CRAFTING_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/craft_selected.png");
    private static final ResourceLocation CODEX_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon.png");
    private static final ResourceLocation CODEX_ICON_SELECTED = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/codexicon_selected.png");
    private static final ResourceLocation HEALTH_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/health.png");

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
    private static final float HEALTH_TEXT_SCALE = 0.86F;

    private enum ScreenMode { INVENTORY, STATUS, CRAFTING, CODEX }
    private enum DragSourceKind { NONE, MAIN, EQUIPMENT }

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
    private float dropPreviewFade = 0.0F;
    private static final long PDA_TRANSITION_NANOS = 820_000_000L;
    private final InventoryPdaPresentationRenderer pdaPresentation =
            new InventoryPdaPresentationRenderer();
    private final long pdaOpenStartedNanos = System.nanoTime();
    private long pdaCloseStartedNanos = -1L;
    private Screen pdaNextScreen;
    private boolean completingPdaClose;
    private boolean pdaOpenStateSent;
    private long pdaLastRenderNanos = System.nanoTime();
    private float pdaDocumentProgress;
    private InventoryPdaPresentationRenderer.Pose pdaPose =
            new InventoryPdaPresentationRenderer.Pose(
                    0.70F, -1.40F, -4.70F,
                    -28.0F, 238.0F, -4.0F, 0.82F);

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
        if (!pdaOpenStateSent) {
            ModNetwork.CHANNEL.sendToServer(
                    InventoryPdaStatePacket.request(true));
            pdaOpenStateSent = true;
        }

        mc.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv -> {
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
            codexPanel.setExpandedBounds(rootX, rootY, rootWidth, rootHeight);
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
        if (pdaCloseStartedNanos > 0L
                && System.nanoTime() - pdaCloseStartedNanos >= PDA_TRANSITION_NANOS) {
            completingPdaClose = true;
            minecraft.setScreen(pdaNextScreen);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        InventoryPdaPresentationRenderer.MappedMouse previousMouse =
                mapPdaMouse(mouseX, mouseY);
        updateDropPreviewFade(isPreviewingWorldDrop(
                previousMouse.x(), previousMouse.y()));
        pdaPose = updatePdaPose(mouseX, mouseY);
        InventoryPdaPresentationRenderer.MappedMouse mappedMouse =
                mapPdaMouse(mouseX, mouseY);
        int uiMouseX = (int) Math.round(mappedMouse.x());
        int uiMouseY = (int) Math.round(mappedMouse.y());

        // Deliberately do not call renderBackground: the world remains clear
        // around the physical device, just as it does in the reference.
        pdaPresentation.captureInterface(g, () ->
                renderPdaContents(g, uiMouseX, uiMouseY, partialTick));
        pdaPresentation.render(pdaPose, pdaPackedLight(), width, height,
                rootX, rootY, rootWidth, rootHeight);
    }

    private void renderPdaContents(GuiGraphics g, int mouseX, int mouseY,
            float partialTick) {
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

    private InventoryPdaPresentationRenderer.Pose updatePdaPose(
            int mouseX, int mouseY) {
        long now = System.nanoTime();
        float frameSeconds = Math.min(0.1F,
                (now - pdaLastRenderNanos) / 1_000_000_000.0F);
        pdaLastRenderNanos = now;
        float documentTarget = mode == ScreenMode.CODEX && codexPanel != null
                && codexPanel.isExpandedImage() ? 1.0F : 0.0F;
        float documentBlend = 1.0F
                - (float) Math.exp(-10.0F * frameSeconds);
        pdaDocumentProgress += (documentTarget - pdaDocumentProgress)
                * documentBlend;

        float progress = presentationProgress();
        float travel = easeOutCubic(progress);
        float settle = easeOutBack(progress);
        float turn = smootherStep(clamp01((progress - 0.14F) / 0.72F));

        float x = lerp(0.70F, -0.03F, travel);
        float y = lerp(-1.40F, -0.02F, settle);
        float depth = lerp(-4.70F, -1.88F, settle);
        float pitch = lerp(-28.0F, 2.0F, turn);
        // 180 degrees faces the authored negative-Z screen toward the camera.
        float yaw = lerp(238.0F, 176.5F, turn);
        float roll = lerp(-4.0F, -90.0F, turn);

        float idleWeight = smootherStep(clamp01(
                (progress - 0.86F) / 0.14F));
        float idleTime = now / 1_000_000_000.0F;
        x += (float) Math.sin(idleTime * 0.72F) * 0.004F * idleWeight;
        y += (float) Math.sin(idleTime * 1.08F) * 0.006F * idleWeight;
        pitch += (float) Math.sin(idleTime * 0.83F) * 0.35F * idleWeight;
        yaw += (float) Math.cos(idleTime * 0.61F) * 0.32F * idleWeight;

        if (dropPreviewFade > 0.001F) {
            float dx = (mouseX - width * 0.5F)
                    / Math.max(1.0F, width * 0.5F);
            float dy = (mouseY - height * 0.5F)
                    / Math.max(1.0F, height * 0.5F);
            x -= dx * 0.13F * dropPreviewFade;
            y += dy * 0.10F * dropPreviewFade;
            yaw += dx * 5.0F * dropPreviewFade;
            pitch += dy * 3.5F * dropPreviewFade;
        }

        if (pdaDocumentProgress > 0.001F) {
            float documentEase = pdaDocumentProgress * pdaDocumentProgress
                    * (3.0F - 2.0F * pdaDocumentProgress);
            x = lerp(x, 0.0F, documentEase);
            y = lerp(y, -0.01F, documentEase);
            depth = lerp(depth, -1.68F, documentEase);
            pitch = lerp(pitch, 4.0F, documentEase);
            yaw = lerp(yaw, 180.0F, documentEase);
            roll = lerp(roll, 0.0F, documentEase);
        }

        return new InventoryPdaPresentationRenderer.Pose(x, y, depth,
                pitch, yaw, roll, 0.82F);
    }

    private int pdaPackedLight() {
        if (minecraft == null || minecraft.level == null
                || minecraft.player == null) return LightTexture.FULL_BRIGHT;
        return LevelRenderer.getLightColor(minecraft.level,
                minecraft.player.blockPosition());
    }

    private InventoryPdaPresentationRenderer.MappedMouse mapPdaMouse(
            double mouseX, double mouseY) {
        return pdaPresentation.mapMouse(pdaPose, mouseX, mouseY,
                width, height, rootX, rootY, rootWidth, rootHeight);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float smootherStep(float value) {
        return value * value * value
                * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1.0F;
        float overshoot = 1.35F;
        return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted
                + overshoot * shifted * shifted;
    }

    private float presentationProgress() {
        long now = System.nanoTime();
        if (pdaCloseStartedNanos > 0L) {
            return 1.0F - Math.min(1.0F,
                    (now - pdaCloseStartedNanos) / (float) PDA_TRANSITION_NANOS);
        }
        return Math.min(1.0F,
                (now - pdaOpenStartedNanos) / (float) PDA_TRANSITION_NANOS);
    }

    public boolean beginPdaClose(Screen nextScreen) {
        if (completingPdaClose) return false;
        if (pdaCloseStartedNanos < 0L) {
            captureSessionState();
            pdaNextScreen = nextScreen;
            pdaCloseStartedNanos = System.nanoTime();
            clearDragSource();
            if (contextMenu != null) contextMenu.close();
            if (pdaOpenStateSent) {
                ModNetwork.CHANNEL.sendToServer(
                        InventoryPdaStatePacket.request(false));
                pdaOpenStateSent = false;
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        beginPdaClose(null);
    }

    public boolean isPdaInteractive() {
        return pdaCloseStartedNanos < 0L && presentationProgress() >= 0.98F;
    }

    private void updateDropPreviewFade(boolean targetVisible) {
        float target = targetVisible ? 1.0F : 0.0F;
        float speed = targetVisible ? 0.055F : 0.070F;
        dropPreviewFade += (target - dropPreviewFade) * speed;
        if (Math.abs(dropPreviewFade - target) < 0.008F) dropPreviewFade = target;
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
        return color;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isPdaInteractive()) return true;
        InventoryPdaPresentationRenderer.MappedMouse mapped =
                mapPdaMouse(mouseX, mouseY);
        mouseX = mapped.x();
        mouseY = mapped.y();
        if (mode == ScreenMode.CODEX && codexPanel != null && codexPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (mode == ScreenMode.STATUS && statusPanel != null && statusPanel.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (mode == ScreenMode.INVENTORY && itemList != null && itemList.isMouseOver(mouseX, mouseY)) return itemList.mouseScrolled(delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isPdaInteractive()) return true;
        InventoryPdaPresentationRenderer.MappedMouse mapped =
                mapPdaMouse(mouseX, mouseY);
        mouseX = mapped.x();
        mouseY = mapped.y();
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
        if (!isPdaInteractive()) return true;
        InventoryPdaPresentationRenderer.MappedMouse mapped =
                mapPdaMouse(mouseX, mouseY);
        InventoryPdaPresentationRenderer.MappedMouse previous =
                mapPdaMouse(mouseX - dragX, mouseY - dragY);
        mouseX = mapped.x();
        mouseY = mapped.y();
        dragX = mapped.x() - previous.x();
        dragY = mapped.y() - previous.y();
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
        if (!isPdaInteractive()) return true;
        InventoryPdaPresentationRenderer.MappedMouse mapped =
                mapPdaMouse(mouseX, mouseY);
        mouseX = mapped.x();
        mouseY = mapped.y();
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
        if ("Consumable".equals(itemType) || "Usable".equals(itemType)
                || "Placeable".equals(itemType)) {
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
        pdaPresentation.close();
        if (pdaOpenStateSent) {
            ModNetwork.CHANNEL.sendToServer(
                    InventoryPdaStatePacket.request(false));
            pdaOpenStateSent = false;
        }
        super.removed();
    }

    private static boolean rememberUiState() {
        return ScpClassifiedDirectiveModulesConfig.get().inventory.enabled
                && ScpClassifiedDirectiveModulesConfig.get().inventory.rememberUiState;
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
