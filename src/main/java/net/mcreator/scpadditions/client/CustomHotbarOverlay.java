package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/** Rotating SCP Additions hotbar with the active item anchored on the right. */
public final class CustomHotbarOverlay {
    private static final ResourceLocation PICKUP_ICON = new ResourceLocation(
            "scpinventory", "textures/gui/pickup.png");
    private static final int PICKUP_ICON_SOURCE_SIZE = 128;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 24;
    private static final int SELECTED_SLOT_SIZE = 29;
    private static final int CELL_GAP = 4;
    private static final int MIN_CELL_WIDTH = 27;
    private static final int MAX_CELL_WIDTH = 82;
    private static final int INFO_GAP = 11;
    private static final int MIN_INFO_WIDTH = 150;
    private static final int MAX_INFO_WIDTH = 220;
    private static final int MAX_DETAIL_LINES = 3;
    private static final float LABEL_SCALE = 0.75F;
    private static final float DETAIL_SCALE = 0.70F;
    private static final float MIN_NAME_SCALE = 0.68F;

    private static final int SLOT_BACKGROUND = 0x70081022;
    private static final int SELECTED_BACKGROUND = 0xA0081022;
    private static final int SELECTED_BORDER = 0xFFC59A2A;
    private static final int CATEGORY_TEXT = 0xD89EA6B3;
    private static final int SELECTED_CATEGORY_TEXT = 0xF2E5D49A;
    private static final int ITEM_NAME_TEXT = 0xF2E5D49A;
    private static final int ITEM_DETAIL_TEXT = 0xD0A9B0BC;

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
        Inventory inventory = player.getInventory();
        List<HotbarEntry> occupied = collectEntries(inventory, font);
        if (occupied.isEmpty()) return;

        int selectedSlot = inventory.selected;
        boolean emptySelected = selectedSlot < 0
                || selectedSlot >= inventory.items.size()
                || inventory.items.get(selectedSlot).isEmpty();
        List<HotbarEntry> entries = rotateSelectionToRight(
                occupied, selectedSlot, emptySelected);
        HotbarEntry selectedEntry = entries.get(entries.size() - 1);
        ItemInfo info = itemInfo(selectedEntry.stack(), player, font);

        int availableWidth = Math.max(1, screenWidth - 16);
        int desiredInfoWidth = info.visible()
                ? Math.min(MAX_INFO_WIDTH,
                        Math.max(MIN_INFO_WIDTH, info.naturalWidth() + 8))
                : 0;
        int maximumListWidth = desiredInfoWidth > 0
                ? Math.max(MIN_CELL_WIDTH,
                        availableWidth - INFO_GAP - desiredInfoWidth)
                : availableWidth;
        fitCells(entries, maximumListWidth);

        int listWidth = totalWidth(entries);
        int infoWidth = desiredInfoWidth > 0
                ? Math.max(0, Math.min(desiredInfoWidth,
                        availableWidth - listWidth - INFO_GAP))
                : 0;
        int combinedWidth = listWidth
                + (infoWidth > 0 ? INFO_GAP + infoWidth : 0);
        int cursorX = (screenWidth - combinedWidth) / 2;
        int bottom = screenHeight - 8;
        int selectedRight = cursorX;
        int selectedTop = bottom - SELECTED_SLOT_SIZE;

        for (HotbarEntry entry : entries) {
            boolean selected = entry.selected();
            int cellCenterX = cursorX + entry.cellWidth() / 2;
            int slotSize = selected ? SELECTED_SLOT_SIZE : SLOT_SIZE;
            int slotX = cellCenterX - slotSize / 2;
            int slotY = bottom - slotSize;
            int labelY = slotY - 10;

            if (!entry.category().getString().isBlank()) {
                drawScaledCentered(graphics, font,
                        entry.category().getString(), cellCenterX, labelY,
                        LABEL_SCALE, selected
                                ? SELECTED_CATEGORY_TEXT : CATEGORY_TEXT);
            }
            drawSlot(graphics, slotX, slotY, slotSize, selected);
            if (entry.stack().isEmpty()) {
                drawPickupIcon(graphics, cellCenterX,
                        slotY + slotSize / 2);
            } else {
                drawItem(graphics, font, entry.stack(), cellCenterX,
                        slotY + slotSize / 2, selected);
            }

            if (selected) {
                selectedRight = slotX + slotSize;
                selectedTop = slotY;
            }
            cursorX += entry.cellWidth() + CELL_GAP;
        }

        if (infoWidth > 0 && info.visible()) {
            drawItemInfo(graphics, font, info,
                    selectedRight + INFO_GAP, selectedTop + 3, infoWidth);
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
            entries.add(new HotbarEntry(slot, stack, category,
                    cellWidth, false));
        }
        return entries;
    }

    private static List<HotbarEntry> rotateSelectionToRight(
            List<HotbarEntry> occupied, int selectedSlot,
            boolean emptySelected) {
        List<HotbarEntry> rotated = new ArrayList<>();
        if (emptySelected) {
            for (HotbarEntry entry : occupied) {
                rotated.add(entry.withSelected(false));
            }
            rotated.add(new HotbarEntry(selectedSlot, ItemStack.EMPTY,
                    Component.empty(), MIN_CELL_WIDTH, true));
            return rotated;
        }

        int selectedIndex = -1;
        for (int i = 0; i < occupied.size(); i++) {
            if (occupied.get(i).slot() == selectedSlot) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex < 0) {
            for (HotbarEntry entry : occupied) {
                rotated.add(entry.withSelected(false));
            }
            HotbarEntry last = rotated.remove(rotated.size() - 1);
            rotated.add(last.withSelected(true));
            return rotated;
        }

        for (int offset = 1; offset < occupied.size(); offset++) {
            int index = (selectedIndex + offset) % occupied.size();
            rotated.add(occupied.get(index).withSelected(false));
        }
        rotated.add(occupied.get(selectedIndex).withSelected(true));
        return rotated;
    }

    private static ItemInfo itemInfo(ItemStack stack, LocalPlayer player,
            Font font) {
        if (stack == null || stack.isEmpty()) {
            return ItemInfo.hidden();
        }

        String name = stack.getHoverName().getString();
        List<String> details = new ArrayList<>();
        List<Component> tooltip = stack.getTooltipLines(player,
                TooltipFlag.Default.NORMAL);
        for (int i = 1; i < tooltip.size() && details.size() < 2; i++) {
            String line = tooltip.get(i).getString().trim();
            if (!line.isBlank()) details.add(line);
        }

        int width = font.width(ScpFonts.roboto(name));
        for (String detail : details) {
            width = Math.max(width, Math.round(
                    font.width(ScpFonts.roboto(detail)) * DETAIL_SCALE));
        }
        return new ItemInfo(name, List.copyOf(details), width, true);
    }

    private static void fitCells(List<HotbarEntry> entries,
            int maximumContentWidth) {
        while (totalWidth(entries) > maximumContentWidth) {
            boolean reduced = false;
            for (int i = 0; i < entries.size(); i++) {
                HotbarEntry entry = entries.get(i);
                if (entry.cellWidth() <= MIN_CELL_WIDTH) continue;
                entries.set(i, entry.withCellWidth(entry.cellWidth() - 1));
                reduced = true;
                if (totalWidth(entries) <= maximumContentWidth) return;
            }
            if (!reduced) return;
        }
    }

    private static int totalWidth(List<HotbarEntry> entries) {
        int width = Math.max(0, entries.size() - 1) * CELL_GAP;
        for (HotbarEntry entry : entries) width += entry.cellWidth();
        return width;
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
        if (!selected) {
            RenderSystem.setShaderColor(0.66F, 0.66F, 0.66F, 0.66F);
            graphics.renderItem(stack, itemX, itemY);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.renderItemDecorations(font, stack, itemX, itemY);
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(1.14F, 1.14F, 1.0F);
        graphics.renderItem(stack, -8, -8);
        graphics.pose().popPose();
        graphics.renderItemDecorations(font, stack, itemX, itemY);
    }

    private static void drawPickupIcon(GuiGraphics graphics,
            int centerX, int centerY) {
        int size = 22;
        int x = centerX - size / 2;
        int y = centerY - size / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.72F);
        graphics.blit(PICKUP_ICON, x, y, size, size,
                0.0F, 0.0F,
                PICKUP_ICON_SOURCE_SIZE, PICKUP_ICON_SOURCE_SIZE,
                PICKUP_ICON_SOURCE_SIZE, PICKUP_ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawItemInfo(GuiGraphics graphics, Font font,
            ItemInfo info, int x, int y, int availableWidth) {
        drawFitted(graphics, font, info.name(), x, y,
                availableWidth, 1.0F, MIN_NAME_SCALE, ITEM_NAME_TEXT);

        int detailY = y + 12;
        int renderedLines = 0;
        for (String detail : info.details()) {
            for (String line : wrap(font, detail, availableWidth,
                    DETAIL_SCALE)) {
                if (renderedLines >= MAX_DETAIL_LINES) return;
                drawScaled(graphics, font, line, x, detailY,
                        DETAIL_SCALE, ITEM_DETAIL_TEXT);
                detailY += 8;
                renderedLines++;
            }
        }
    }

    private static void drawFitted(GuiGraphics graphics, Font font,
            String text, int x, int y, int availableWidth,
            float preferredScale, float minimumScale, int color) {
        Component component = ScpFonts.roboto(text);
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(preferredScale,
                availableWidth / (float) textWidth);
        scale = Math.max(minimumScale, scale);
        drawScaled(graphics, font, text, x, y, scale, color);
    }

    private static List<String> wrap(Font font, String text,
            int availableWidth, float scale) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        if (remaining.isBlank()) return lines;

        int unscaledWidth = Math.max(1,
                (int) Math.floor(availableWidth / scale));
        while (!remaining.isBlank() && lines.size() < MAX_DETAIL_LINES) {
            if (font.width(ScpFonts.roboto(remaining)) <= unscaledWidth) {
                lines.add(remaining);
                break;
            }

            int split = bestSplit(font, remaining, unscaledWidth);
            if (split <= 0) {
                split = Math.max(1, font.plainSubstrByWidth(
                        remaining, unscaledWidth).length());
            }
            lines.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }
        return lines;
    }

    private static int bestSplit(Font font, String text,
            int unscaledWidth) {
        int best = -1;
        for (int i = 1; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) continue;
            String candidate = text.substring(0, i).trim();
            if (font.width(ScpFonts.roboto(candidate)) <= unscaledWidth) {
                best = i;
            } else {
                break;
            }
        }
        return best;
    }

    private static void drawScaledCentered(GuiGraphics graphics, Font font,
            String text, float centerX, float y, float scale, int color) {
        Component component = ScpFonts.roboto(text);
        float scaledWidth = font.width(component) * scale;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - scaledWidth / 2.0F, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, component, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawScaled(GuiGraphics graphics, Font font,
            String text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, ScpFonts.roboto(text),
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private record HotbarEntry(int slot, ItemStack stack,
                               Component category, int cellWidth,
                               boolean selected) {
        private HotbarEntry withCellWidth(int width) {
            return new HotbarEntry(slot, stack, category, width, selected);
        }

        private HotbarEntry withSelected(boolean value) {
            return new HotbarEntry(slot, stack, category, cellWidth, value);
        }
    }

    private record ItemInfo(String name, List<String> details,
                            int naturalWidth, boolean visible) {
        private static ItemInfo hidden() {
            return new ItemInfo("", List.of(), 0, false);
        }
    }
}
