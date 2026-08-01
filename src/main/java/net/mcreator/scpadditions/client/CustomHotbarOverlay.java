package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** First interpretation of the compact SCP Additions hotbar presentation. */
public final class CustomHotbarOverlay {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 24;
    private static final int SELECTED_SLOT_SIZE = 28;
    private static final int CELL_GAP = 4;
    private static final int MIN_CELL_WIDTH = 34;
    private static final int MAX_CELL_WIDTH = 82;
    private static final float LABEL_SCALE = 0.75F;

    private static final int SLOT_BACKGROUND = 0xD9474A50;
    private static final int SELECTED_BACKGROUND = 0xEE081022;
    private static final int SELECTED_BORDER = 0xFFC59A2A;
    private static final int CATEGORY_TEXT = 0xFFB8BEC8;
    private static final int SELECTED_CATEGORY_TEXT = 0xFFE5D49A;

    private CustomHotbarOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isActiveFor(player)
                || minecraft.options.hideGui
                || minecraft.screen != null) {
            return;
        }

        Font font = minecraft.font;
        List<HotbarEntry> entries = collectEntries(player.getInventory(), font);
        if (entries.isEmpty()) return;

        int maximumContentWidth = Math.max(1, screenWidth - 16);
        fitCells(entries, maximumContentWidth);
        int totalWidth = totalWidth(entries);
        int cursorX = (screenWidth - totalWidth) / 2;
        int bottom = screenHeight - 8;
        int selectedSlot = player.getInventory().selected;

        for (HotbarEntry entry : entries) {
            boolean selected = entry.slot() == selectedSlot;
            int cellCenterX = cursorX + entry.cellWidth() / 2;
            int slotSize = selected ? SELECTED_SLOT_SIZE : SLOT_SIZE;
            int slotX = cellCenterX - slotSize / 2;
            int slotY = bottom - slotSize;
            int labelY = slotY - 10;

            drawCategory(graphics, font, entry.category(), cellCenterX,
                    labelY, selected ? SELECTED_CATEGORY_TEXT : CATEGORY_TEXT);
            drawSlot(graphics, slotX, slotY, slotSize, selected);
            drawItem(graphics, font, entry.stack(), cellCenterX,
                    slotY + slotSize / 2, selected);

            cursorX += entry.cellWidth() + CELL_GAP;
        }
    }

    public static boolean isActiveFor(LocalPlayer player) {
        return player != null
                && !player.isCreative()
                && !player.isSpectator()
                && InventoryModuleRuntimeState.isEnabledForClient()
                && InventoryModuleRuntimeState.customHotbarForClient();
    }

    private static List<HotbarEntry> collectEntries(Inventory inventory,
            Font font) {
        List<HotbarEntry> entries = new ArrayList<>();
        int end = Math.min(HOTBAR_SLOT_COUNT, inventory.items.size());
        for (int slot = 0; slot < end; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (stack.isEmpty()) continue;

            Component category = ScpFonts.roboto(
                    ScpItemClassifier.getDisplayType(stack));
            int naturalWidth = Math.round(font.width(category) * LABEL_SCALE)
                    + 8;
            int cellWidth = Math.max(MIN_CELL_WIDTH,
                    Math.min(MAX_CELL_WIDTH, naturalWidth));
            entries.add(new HotbarEntry(slot, stack, category, cellWidth));
        }
        return entries;
    }

    private static void fitCells(List<HotbarEntry> entries,
            int maximumContentWidth) {
        int current = totalWidth(entries);
        if (current <= maximumContentWidth) return;

        int excess = current - maximumContentWidth;
        int remaining = entries.size();
        for (int i = 0; i < entries.size() && excess > 0; i++) {
            HotbarEntry entry = entries.get(i);
            int fairShare = Math.max(1,
                    (int) Math.ceil(excess / (double) remaining));
            int reduction = Math.min(fairShare,
                    Math.max(0, entry.cellWidth() - MIN_CELL_WIDTH));
            entries.set(i, entry.withCellWidth(entry.cellWidth() - reduction));
            excess -= reduction;
            remaining--;
        }
    }

    private static int totalWidth(List<HotbarEntry> entries) {
        int width = Math.max(0, entries.size() - 1) * CELL_GAP;
        for (HotbarEntry entry : entries) width += entry.cellWidth();
        return width;
    }

    private static void drawCategory(GuiGraphics graphics, Font font,
            Component text, float centerX, float y, int color) {
        float scaledWidth = font.width(text) * LABEL_SCALE;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - scaledWidth / 2.0F, y, 0.0F);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y,
            int size, boolean selected) {
        int right = x + size;
        int bottom = y + size;
        graphics.fill(x, y, right, bottom,
                selected ? SELECTED_BACKGROUND : SLOT_BACKGROUND);
        if (!selected) return;

        graphics.fill(x, y, right, y + 1, SELECTED_BORDER);
        graphics.fill(x, bottom - 1, right, bottom, SELECTED_BORDER);
        graphics.fill(x, y, x + 1, bottom, SELECTED_BORDER);
        graphics.fill(right - 1, y, right, bottom, SELECTED_BORDER);
    }

    private static void drawItem(GuiGraphics graphics, Font font,
            ItemStack stack, int centerX, int centerY, boolean selected) {
        int itemX = centerX - 8;
        int itemY = centerY - 8;
        if (selected) {
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 0.0F);
            graphics.pose().scale(1.125F, 1.125F, 1.0F);
            graphics.renderItem(stack, -8, -8);
            graphics.pose().popPose();
        } else {
            graphics.renderItem(stack, itemX, itemY);
        }
        graphics.renderItemDecorations(font, stack, itemX, itemY);
    }

    private record HotbarEntry(int slot, ItemStack stack,
                               Component category, int cellWidth) {
        private HotbarEntry withCellWidth(int width) {
            return new HotbarEntry(slot, stack, category, width);
        }
    }
}
