package net.mcreator.scpadditions.inventory.client;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * First visual checkpoint of the integrated SCP Inventory.
 *
 * The screen is intentionally read-only until server-authoritative action
 * packets, equipment and the sensitive USABLE session flow are ported. It uses
 * the final background/font assets and the synchronized player capability.
 */
public final class ScpInventoryScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            "scpinventory", "textures/gui/inventory_background.png");
    private static final ResourceLocation ROBOTO = new ResourceLocation(
            "scpinventory", "roboto");

    private static final int SOURCE_WIDTH = 1406;
    private static final int SOURCE_HEIGHT = 1080;
    private static final int TEXT = 0xFFBFC4C4;
    private static final int TEXT_DIM = 0xFF737B7D;
    private static final int PANEL = 0x88434A4C;
    private static final int SLOT = 0x99292F31;
    private static final int SLOT_BORDER = 0xAA687174;
    private static final int TAB_ACTIVE = 0x997F8A8D;
    private static final int TAB_INACTIVE = 0x55434A4C;

    private IScpInventory inventory;
    private boolean showingKeys;
    private int rootX;
    private int rootY;
    private int rootWidth;
    private int rootHeight;
    private int inventoryTabX;
    private int keysTabX;
    private int tabY;
    private int tabWidth;
    private int tabHeight;

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
        int margin = 20;
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

        tabWidth = Math.max(72, Math.round(rootWidth * 0.16F));
        tabHeight = Math.max(18, Math.round(rootHeight * 0.034F));
        tabY = rootY + Math.round(rootHeight * 0.155F);
        inventoryTabX = rootX + Math.round(rootWidth * 0.085F);
        keysTabX = inventoryTabX + tabWidth + 4;
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

        Component title = font("SCP INVENTORY");
        graphics.drawCenteredString(font, title,
                rootX + rootWidth / 2,
                rootY + Math.round(rootHeight * 0.095F), TEXT);

        drawTab(graphics, inventoryTabX, tabY,
                Component.translatable("screen.scp_additions.scp_inventory.inventory"),
                !showingKeys);
        drawTab(graphics, keysTabX, tabY,
                Component.translatable("screen.scp_additions.scp_inventory.keys"),
                showingKeys);

        int panelX = rootX + Math.round(rootWidth * 0.075F);
        int panelY = tabY + tabHeight + 8;
        int panelWidth = rootWidth - Math.round(rootWidth * 0.15F);
        int panelHeight = rootY + rootHeight
                - Math.round(rootHeight * 0.13F) - panelY;
        graphics.fill(panelX, panelY, panelX + panelWidth,
                panelY + panelHeight, PANEL);

        if (inventory == null) {
            graphics.drawCenteredString(font,
                    font("Waiting for inventory synchronization..."),
                    panelX + panelWidth / 2, panelY + 18, TEXT_DIM);
        } else if (showingKeys) {
            renderStackGrid(graphics, inventory.getKeys(), panelX, panelY,
                    panelWidth, panelHeight, "Key");
        } else {
            renderStackGrid(graphics, inventory.getInventory(), panelX, panelY,
                    Math.round(panelWidth * 0.66F), panelHeight, null);
            renderEquipment(graphics,
                    panelX + Math.round(panelWidth * 0.68F), panelY,
                    Math.round(panelWidth * 0.30F), panelHeight);
        }

        graphics.drawCenteredString(font,
                font("Read-only integration checkpoint"),
                rootX + rootWidth / 2,
                rootY + rootHeight - Math.round(rootHeight * 0.085F),
                TEXT_DIM);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTab(GuiGraphics graphics, int x, int y,
            Component label, boolean active) {
        graphics.fill(x, y, x + tabWidth, y + tabHeight,
                active ? TAB_ACTIVE : TAB_INACTIVE);
        graphics.drawCenteredString(font,
                label.copy().withStyle(style -> style.withFont(ROBOTO)),
                x + tabWidth / 2, y + 5,
                active ? 0xFFF0F2F2 : TEXT_DIM);
    }

    private void renderStackGrid(GuiGraphics graphics, List<ItemStack> stacks,
            int x, int y, int width, int height, String fixedType) {
        int columns = width >= 360 ? 2 : 1;
        int gap = 6;
        int cellWidth = (width - 20 - gap * (columns - 1)) / columns;
        int cellHeight = 31;
        int startX = x + 10;
        int startY = y + 12;
        int visibleRows = Math.max(1, (height - 22) / cellHeight);
        int max = Math.min(stacks.size(), visibleRows * columns);

        for (int index = 0; index < max; index++) {
            int column = index % columns;
            int row = index / columns;
            int cellX = startX + column * (cellWidth + gap);
            int cellY = startY + row * cellHeight;
            ItemStack stack = stacks.get(index);

            graphics.fill(cellX, cellY, cellX + cellWidth,
                    cellY + 27, SLOT_BORDER);
            graphics.fill(cellX + 1, cellY + 1, cellX + cellWidth - 1,
                    cellY + 26, SLOT);

            if (stack == null || stack.isEmpty()) {
                graphics.drawString(font, font("Empty"), cellX + 27,
                        cellY + 9, TEXT_DIM, false);
                continue;
            }

            graphics.renderItem(stack, cellX + 5, cellY + 5);
            graphics.renderItemDecorations(font, stack, cellX + 5, cellY + 5);
            String name = stack.getHoverName().getString();
            String clipped = font.plainSubstrByWidth(name,
                    Math.max(12, cellWidth - 34));
            graphics.drawString(font, font(clipped), cellX + 27,
                    cellY + 4, TEXT, false);
            String type = fixedType != null
                    ? fixedType : ScpItemClassifier.getType(stack).getDisplayName();
            graphics.drawString(font, font(type), cellX + 27,
                    cellY + 15, TEXT_DIM, false);
        }
    }

    private void renderEquipment(GuiGraphics graphics, int x, int y,
            int width, int height) {
        graphics.drawCenteredString(font, font("EQUIPMENT"),
                x + width / 2, y + 12, TEXT);
        ScpEquipmentSlot[] slots = {
                ScpEquipmentSlot.HEAD,
                ScpEquipmentSlot.CHEST,
                ScpEquipmentSlot.LEGS,
                ScpEquipmentSlot.FEET,
                ScpEquipmentSlot.ACCESSORY,
                ScpEquipmentSlot.WEAPON
        };
        int rowY = y + 31;
        for (ScpEquipmentSlot slot : slots) {
            ItemStack stack = inventory.getEquipment(slot);
            graphics.fill(x + 4, rowY, x + width - 4, rowY + 28, SLOT_BORDER);
            graphics.fill(x + 5, rowY + 1, x + width - 5, rowY + 27, SLOT);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 9, rowY + 6);
                graphics.renderItemDecorations(font, stack, x + 9, rowY + 6);
            }
            graphics.drawString(font, font(slot.getDisplayName()),
                    x + 31, rowY + 9, stack.isEmpty() ? TEXT_DIM : TEXT, false);
            rowY += 33;
            if (rowY + 28 > y + height) break;
        }
    }

    private static Component font(String text) {
        return Component.literal(text)
                .withStyle(style -> style.withFont(ROBOTO));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY,
                inventoryTabX, tabY, tabWidth, tabHeight)) {
            showingKeys = false;
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY,
                keysTabX, tabY, tabWidth, tabHeight)) {
            showingKeys = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
}
