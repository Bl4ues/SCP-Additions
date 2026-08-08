package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only SCP Inventory-style list used by storage container screens.
 *
 * <p>The backing list may be a live view over an open container menu, so this
 * component never caches ItemStacks themselves. It only caches the indexes of
 * non-empty entries for the current frame.</p>
 */
public final class StorageListView {
    private static final int ROW_HEIGHT = 40;
    private static final int ICON_BOX_SIZE = 24;
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int ICON_BOX = 0x66303638;
    private static final int ICON_BORDER = 0xAA6A6C6C;
    private static final int SCROLL_TRACK = 0x44000000;
    private static final int SCROLL_THUMB = 0xAA6A6C6C;
    private static final int TEXT_RIGHT_PADDING = 18;
    private static final int MARQUEE_DELAY_MS = 650;
    private static final int MARQUEE_EDGE_PAUSE_MS = 750;
    private static final int MARQUEE_SPEED_PX_PER_SECOND = 26;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final int x;
    private final int y;
    private final int width;
    private final int visibleRows;
    private final List<ItemStack> items;
    private final String fixedTypeLabel;

    private final List<Integer> nonEmptySlots = new ArrayList<>();
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int marqueeHoverSlot = -1;
    private long marqueeHoverStartedMs;

    public StorageListView(int x, int y, int width, int visibleRows,
                           List<ItemStack> items, String fixedTypeLabel) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.visibleRows = Math.max(1, visibleRows);
        this.items = items;
        this.fixedTypeLabel = fixedTypeLabel;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Integer> slots = rebuildNonEmptySlots();
        clampScroll(slots.size());
        updateMarqueeHover(slots, mouseX, mouseY);

        for (int row = 0; row < visibleRows; row++) {
            int visibleIndex = scrollOffset + row;
            if (visibleIndex >= slots.size()) {
                break;
            }

            int sourceIndex = slots.get(visibleIndex);
            ItemStack stack = safeGet(sourceIndex);
            int rowY = y + row * ROW_HEIGHT;
            renderRow(graphics, stack, rowY,
                    sourceIndex == marqueeHoverSlot);
        }

        renderScrollbar(graphics, slots.size());
    }

    private void renderRow(GuiGraphics graphics, ItemStack stack, int rowY,
                           boolean hovered) {
        int iconX = x + 10;
        int iconY = rowY + 7;
        int textX = x + 44;
        int textMaxWidth = Math.max(24,
                x + width - TEXT_RIGHT_PADDING - textX);

        if (!stack.isEmpty()) {
            drawIconFrame(graphics, iconX, iconY);
            graphics.renderItem(stack, iconX + 4, iconY + 4);
            ItemStack decorationStack = stack;
            if (stack.getCount() != 1) {
                decorationStack = stack.copy();
                decorationStack.setCount(1);
            }
            graphics.renderItemDecorations(minecraft.font, decorationStack,
                    iconX + 4, iconY + 4);
            renderStackCount(graphics, stack, iconX + 4, iconY + 4);
        }

        String secondary = secondaryText(stack);
        int nameY = secondary.isEmpty() ? rowY + 14 : rowY + 8;
        drawOverflowName(graphics, stack.getHoverName().getString(),
                textX, nameY, textMaxWidth, hovered, TEXT_WHITE);
        if (!secondary.isEmpty()) {
            drawTrimmedText(graphics, secondary, textX, rowY + 21,
                    textMaxWidth, TEXT_GRAY);
        }

        int lineY = rowY + ROW_HEIGHT - 1;
        graphics.fill(x + 2, lineY, x + width - 8, lineY + 1, LINE_GRAY);
    }

    private void renderStackCount(GuiGraphics graphics, ItemStack stack,
                                  int itemX, int itemY) {
        if (stack == null || stack.isEmpty() || stack.getCount() == 1) {
            return;
        }

        Component count = ScpFonts.roboto(Integer.toString(stack.getCount()));
        int countX = itemX + 19 - 2 - minecraft.font.width(count);
        int countY = itemY + 9;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(minecraft.font, count, countX, countY,
                0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private String secondaryText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if ("Key".equals(fixedTypeLabel)) {
            return keyDescription(stack);
        }
        if (fixedTypeLabel != null && !fixedTypeLabel.isBlank()) {
            return fixedTypeLabel;
        }
        return ScpItemClassifier.getDisplayType(stack);
    }

    private String keyDescription(ItemStack stack) {
        if (minecraft.player == null) {
            return "Key";
        }

        List<Component> tooltip = stack.getTooltipLines(
                minecraft.player, TooltipFlag.Default.NORMAL);
        for (int i = 1; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "Key";
    }

    private void drawIconFrame(GuiGraphics graphics, int left, int top) {
        int right = left + ICON_BOX_SIZE;
        int bottom = top + ICON_BOX_SIZE;
        graphics.fill(left, top, right, bottom, ICON_BOX);
        graphics.fill(left, top, right, top + 1, ICON_BORDER);
        graphics.fill(left, bottom - 1, right, bottom, ICON_BORDER);
        graphics.fill(left, top, left + 1, bottom, ICON_BORDER);
        graphics.fill(right - 1, top, right, bottom, ICON_BORDER);
    }

    private void drawOverflowName(GuiGraphics graphics, String text,
                                  int textX, int textY, int maxWidth,
                                  boolean hovered, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }

        int width = minecraft.font.width(ScpFonts.roboto(text));
        if (width <= maxWidth) {
            graphics.drawString(minecraft.font, ScpFonts.roboto(text),
                    textX, textY, color, false);
            return;
        }

        long now = System.currentTimeMillis();
        long hoverAge = hovered && marqueeHoverStartedMs > 0
                ? now - marqueeHoverStartedMs : 0L;
        if (!hovered || hoverAge < MARQUEE_DELAY_MS) {
            drawTrimmedText(graphics, text, textX, textY, maxWidth, color);
            return;
        }

        int overflow = Math.max(1, width - maxWidth);
        long travelMs = Math.max(1200L,
                Math.round(overflow * 1000.0D
                        / MARQUEE_SPEED_PX_PER_SECOND));
        long cycleMs = travelMs + MARQUEE_EDGE_PAUSE_MS * 2L;
        long time = (hoverAge - MARQUEE_DELAY_MS) % cycleMs;

        int offset;
        if (time < MARQUEE_EDGE_PAUSE_MS) {
            offset = 0;
        } else if (time < MARQUEE_EDGE_PAUSE_MS + travelMs) {
            offset = Math.round(overflow
                    * ((time - MARQUEE_EDGE_PAUSE_MS)
                    / (float) travelMs));
        } else {
            offset = overflow;
        }

        graphics.enableScissor(textX, textY - 2,
                textX + maxWidth,
                textY + minecraft.font.lineHeight + 2);
        graphics.drawString(minecraft.font, ScpFonts.roboto(text),
                textX - offset, textY, color, false);
        graphics.disableScissor();
    }

    private void drawTrimmedText(GuiGraphics graphics, String text,
                                 int textX, int textY, int maxWidth,
                                 int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }

        graphics.drawString(minecraft.font,
                ScpFonts.roboto(trimToWidth(text, maxWidth)),
                textX, textY, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (minecraft.font.width(ScpFonts.roboto(text)) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = minecraft.font.width(ScpFonts.roboto(ellipsis));
        if (maxWidth <= ellipsisWidth) {
            return "";
        }

        return minecraft.font.plainSubstrByWidth(
                text, maxWidth - ellipsisWidth).trim() + ellipsis;
    }

    private List<Integer> rebuildNonEmptySlots() {
        nonEmptySlots.clear();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = safeGet(i);
            if (!stack.isEmpty()) {
                nonEmptySlots.add(i);
            }
        }
        return nonEmptySlots;
    }

    private ItemStack safeGet(int index) {
        if (index < 0 || index >= items.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(index);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private void updateMarqueeHover(List<Integer> slots,
                                    int mouseX, int mouseY) {
        int hovered = getHoveredIndex(slots, mouseX, mouseY);
        if (hovered != marqueeHoverSlot) {
            marqueeHoverSlot = hovered;
            marqueeHoverStartedMs = hovered >= 0
                    ? System.currentTimeMillis() : 0L;
        }
    }

    public boolean mouseScrolled(double delta) {
        int total = rebuildNonEmptySlots().size();
        if (delta < 0) {
            scrollOffset++;
        } else if (delta > 0) {
            scrollOffset--;
        }
        clampScroll(total);
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY,
                                         int button) {
        if (button != 0 || !isMouseOverScrollbar(mouseX, mouseY)) {
            return false;
        }
        draggingScrollbar = true;
        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseDraggedScrollbar(double mouseY) {
        if (!draggingScrollbar) {
            return false;
        }
        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleasedScrollbar(int button) {
        if (button != 0 || !draggingScrollbar) {
            return false;
        }
        draggingScrollbar = false;
        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int total = rebuildNonEmptySlots().size();
        if (total <= visibleRows) {
            scrollOffset = 0;
            return;
        }

        int thumbHeight = thumbHeight(total);
        int trackHeight = scrollbarHeight();
        int travel = Math.max(1, trackHeight - thumbHeight);
        int maxScroll = Math.max(1, total - visibleRows);
        double relative = (mouseY - y - thumbHeight / 2.0D) / travel;
        scrollOffset = (int) Math.round(relative * maxScroll);
        clampScroll(total);
    }

    public int getClickedIndex(double mouseX, double mouseY) {
        return getHoveredIndex(rebuildNonEmptySlots(), mouseX, mouseY);
    }

    private int getHoveredIndex(List<Integer> slots,
                                double mouseX, double mouseY) {
        if (draggingScrollbar
                || mouseX < x || mouseX > x + width
                || mouseY < y || mouseY > y + scrollbarHeight()) {
            return -1;
        }

        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        int visibleIndex = scrollOffset + row;
        if (visibleIndex < 0 || visibleIndex >= slots.size()) {
            return -1;
        }
        return slots.get(visibleIndex);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width + 10
                && mouseY >= y && mouseY <= y + scrollbarHeight();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        scrollOffset = offset;
        clampScroll(rebuildNonEmptySlots().size());
    }

    public int nonEmptyCount() {
        return rebuildNonEmptySlots().size();
    }

    private void renderScrollbar(GuiGraphics graphics, int total) {
        if (total <= visibleRows) {
            return;
        }

        int trackX = x + width + 4;
        int trackHeight = scrollbarHeight();
        int thumbHeight = thumbHeight(total);
        int thumbY = thumbY(total, thumbHeight);
        graphics.fill(trackX, y, trackX + 5, y + trackHeight,
                SCROLL_TRACK);
        graphics.fill(trackX, thumbY, trackX + 5,
                thumbY + thumbHeight, SCROLL_THUMB);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int total = rebuildNonEmptySlots().size();
        if (total <= visibleRows) {
            return false;
        }

        int trackX = x + width + 4;
        return mouseX >= trackX - 3 && mouseX <= trackX + 8
                && mouseY >= y && mouseY <= y + scrollbarHeight();
    }

    private int scrollbarHeight() {
        return visibleRows * ROW_HEIGHT;
    }

    private int thumbHeight(int total) {
        int trackHeight = scrollbarHeight();
        return Math.max(18, visibleRows * trackHeight / total);
    }

    private int thumbY(int total, int thumbHeight) {
        int maxScroll = Math.max(1, total - visibleRows);
        int travel = Math.max(1, scrollbarHeight() - thumbHeight);
        return y + scrollOffset * travel / maxScroll;
    }

    private void clampScroll(int total) {
        int max = Math.max(0, total - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }
}
