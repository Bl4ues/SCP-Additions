package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

public class ScrollableItemList {

    private static final int ROW_HEIGHT = 40;
    private static final int MAX_VISIBLE = 7;
    private static final int ICON_BOX_SIZE = 24;
    private static final int DROP_HIT_WIDTH = 24;
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
    private static final float SOLID_ITEM_RENDER_ALPHA_THRESHOLD = 0.72F;

    private final Minecraft mc = Minecraft.getInstance();

    private final int x;
    private final int y;
    private final int width;

    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int marqueeHoverSlot = -1;
    private long marqueeHoverStartedMs = 0L;

    private final List<ItemStack> items;
    private final IScpInventory inventory;
    private final String fixedTypeLabel;
    private final List<Integer> nonEmptySlots = new ArrayList<>();

    public ScrollableItemList(int x, int y, int width, List<ItemStack> items, IScpInventory inventory) {
        this(x, y, width, items, inventory, null);
    }

    public ScrollableItemList(int x, int y, int width, List<ItemStack> items, IScpInventory inventory, String fixedTypeLabel) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.items = items;
        this.inventory = inventory;
        this.fixedTypeLabel = fixedTypeLabel;
    }

    private List<Integer> rebuildNonEmptySlots() {
        nonEmptySlots.clear();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                nonEmptySlots.add(i);
            }
        }
        return nonEmptySlots;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        render(g, mouseX, mouseY, 1.0F);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float alpha) {
        List<Integer> slots = rebuildNonEmptySlots();
        updateMarqueeHover(slots, mouseX, mouseY);

        for (int i = 0; i < MAX_VISIBLE; i++) {
            int listIndex = scrollOffset + i;
            if (listIndex >= slots.size()) break;

            int slotIndex = slots.get(listIndex);
            ItemStack stack = items.get(slotIndex);
            int rowY = y + (i * ROW_HEIGHT);
            boolean hovered = slotIndex == marqueeHoverSlot;

            renderRow(g, stack, slotIndex, rowY, hovered, alpha);
        }

        renderScrollbar(g, slots.size(), alpha);
    }

    private void renderRow(GuiGraphics g, ItemStack stack, int slotIndex, int rowY, boolean hovered, float alpha) {
        boolean keyList = isKeyList();
        int dropX = x + 4;
        int iconX = x + 28;
        int textX = x + 62;
        int iconY = rowY + 7;
        int textMaxWidth = Math.max(24, x + width - TEXT_RIGHT_PADDING - textX);

        if (!keyList) {
            g.drawString(mc.font, ScpFonts.roboto("X"), dropX, rowY + 14, applyAlpha(TEXT_WHITE, alpha), false);
        }

        if (!stack.isEmpty()) {
            drawIconFrame(g, iconX, iconY, alpha);
            if (alpha >= SOLID_ITEM_RENDER_ALPHA_THRESHOLD) {
                g.renderItem(stack, iconX + 4, iconY + 4);
            }
        }

        if (keyList) {
            String description = getKeyDescription(stack);
            int nameY = description.isEmpty() ? rowY + 14 : rowY + 8;
            drawOverflowName(g, stack.getHoverName().getString(), textX, nameY, textMaxWidth, hovered, applyAlpha(TEXT_WHITE, alpha));
            if (!description.isEmpty()) {
                drawTrimmedText(g, description, textX, rowY + 21, textMaxWidth, applyAlpha(TEXT_GRAY, alpha));
            }
        } else {
            drawOverflowName(g, stack.getHoverName().getString(), textX, rowY + 8, textMaxWidth, hovered, applyAlpha(TEXT_WHITE, alpha));
            drawTrimmedText(g, getTypeLabel(slotIndex), textX, rowY + 21, textMaxWidth, applyAlpha(TEXT_GRAY, alpha));
        }

        int lineY = rowY + ROW_HEIGHT - 1;
        g.fill(x + 18, lineY, x + width - 18, lineY + 1, applyAlpha(LINE_GRAY, alpha));
    }

    private String getKeyDescription(ItemStack stack) {
        if (stack.isEmpty() || mc.player == null) {
            return "";
        }

        List<Component> lines = stack.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL);
        for (int i = 1; i < lines.size(); i++) {
            String text = lines.get(i).getString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }

    private void drawIconFrame(GuiGraphics g, int x, int y, float alpha) {
        int right = x + ICON_BOX_SIZE;
        int bottom = y + ICON_BOX_SIZE;

        g.fill(x, y, right, bottom, applyAlpha(ICON_BOX, alpha));
        g.fill(x, y, right, y + 1, applyAlpha(ICON_BORDER, alpha));
        g.fill(x, bottom - 1, right, bottom, applyAlpha(ICON_BORDER, alpha));
        g.fill(x, y, x + 1, bottom, applyAlpha(ICON_BORDER, alpha));
        g.fill(right - 1, y, right, bottom, applyAlpha(ICON_BORDER, alpha));
    }

    private void drawOverflowName(GuiGraphics g, String text, int textX, int textY, int maxWidth, boolean hovered, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }

        int textWidth = mc.font.width(ScpFonts.roboto(text));
        if (textWidth <= maxWidth) {
            g.drawString(mc.font, ScpFonts.roboto(text), textX, textY, color, false);
            return;
        }

        long now = System.currentTimeMillis();
        long hoverAge = hovered && marqueeHoverStartedMs > 0L ? now - marqueeHoverStartedMs : 0L;
        if (!hovered || hoverAge < MARQUEE_DELAY_MS) {
            drawTrimmedText(g, text, textX, textY, maxWidth, color);
            return;
        }

        int overflow = Math.max(1, textWidth - maxWidth);
        long travelMs = Math.max(1200L, Math.round((overflow * 1000.0D) / MARQUEE_SPEED_PX_PER_SECOND));
        long cycleMs = travelMs + (MARQUEE_EDGE_PAUSE_MS * 2L);
        long t = (hoverAge - MARQUEE_DELAY_MS) % cycleMs;

        int offset;
        if (t < MARQUEE_EDGE_PAUSE_MS) {
            offset = 0;
        } else if (t < MARQUEE_EDGE_PAUSE_MS + travelMs) {
            offset = Math.round(overflow * ((t - MARQUEE_EDGE_PAUSE_MS) / (float) travelMs));
        } else {
            offset = overflow;
        }

        g.enableScissor(textX, textY - 2, textX + maxWidth, textY + mc.font.lineHeight + 2);
        g.drawString(mc.font, ScpFonts.roboto(text), textX - offset, textY, color, false);
        g.disableScissor();
    }

    private void drawTrimmedText(GuiGraphics g, String text, int textX, int textY, int maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return;
        }
        g.drawString(mc.font, ScpFonts.roboto(trimToWidth(text, maxWidth)), textX, textY, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (mc.font.width(ScpFonts.roboto(text)) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = mc.font.width(ScpFonts.roboto(ellipsis));
        if (maxWidth <= ellipsisWidth) {
            return "";
        }

        String trimmed = mc.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth).trim();
        return trimmed + ellipsis;
    }

    private void updateMarqueeHover(List<Integer> slots, int mouseX, int mouseY) {
        int hoveredSlot = getHoveredSlot(slots, mouseX, mouseY);
        if (hoveredSlot != marqueeHoverSlot) {
            marqueeHoverSlot = hoveredSlot;
            marqueeHoverStartedMs = hoveredSlot >= 0 ? System.currentTimeMillis() : 0L;
        }
    }

    private int getHoveredSlot(List<Integer> slots, double mouseX, double mouseY) {
        if (draggingScrollbar || mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + getScrollbarHeight()) {
            return -1;
        }

        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        int listIndex = scrollOffset + row;
        if (listIndex < 0 || listIndex >= slots.size()) {
            return -1;
        }
        return slots.get(listIndex);
    }

    private String getTypeLabel(int slotIndex) {
        if (fixedTypeLabel != null) return fixedTypeLabel;
        if (slotIndex < 0 || slotIndex >= items.size()) return "Empty";
        ItemStack stack = items.get(slotIndex);
        return stack.isEmpty() ? "Empty" : ScpItemClassifier.getDisplayType(stack);
    }

    private boolean isKeyList() {
        return "Key".equals(fixedTypeLabel);
    }

    private void renderScrollbar(GuiGraphics g, int totalItems, float alpha) {
        if (totalItems <= MAX_VISIBLE) {
            return;
        }

        int trackX = getScrollbarX();
        int trackY = y;
        int trackHeight = getScrollbarHeight();
        int thumbHeight = getThumbHeight(totalItems);
        int thumbY = getThumbY(totalItems, thumbHeight);

        g.fill(trackX, trackY, trackX + 5, trackY + trackHeight, applyAlpha(SCROLL_TRACK, alpha));
        g.fill(trackX, thumbY, trackX + 5, thumbY + thumbHeight, applyAlpha(SCROLL_THUMB, alpha));
    }

    private int applyAlpha(int color, float alphaMultiplier) {
        int alpha = color >>> 24;
        int adjustedAlpha = Math.max(1, Math.min(255, Math.round(alpha * alphaMultiplier)));
        return (adjustedAlpha << 24) | (color & 0x00FFFFFF);
    }

    public boolean mouseScrolled(double delta) {
        int totalItems = rebuildNonEmptySlots().size();

        if (delta < 0) scrollOffset++;
        if (delta > 0) scrollOffset--;

        clampScroll(totalItems);
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
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
        int totalItems = rebuildNonEmptySlots().size();
        if (totalItems <= MAX_VISIBLE) {
            scrollOffset = 0;
            return;
        }

        int thumbHeight = getThumbHeight(totalItems);
        int trackHeight = getScrollbarHeight();
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int maxScroll = Math.max(1, totalItems - MAX_VISIBLE);
        double relative = (mouseY - y - (thumbHeight / 2.0D)) / thumbTravel;

        scrollOffset = (int) Math.round(relative * maxScroll);
        clampScroll(totalItems);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int totalItems = rebuildNonEmptySlots().size();
        if (totalItems <= MAX_VISIBLE) {
            return false;
        }

        int trackX = getScrollbarX();
        return mouseX >= trackX - 3
                && mouseX <= trackX + 8
                && mouseY >= y
                && mouseY <= y + getScrollbarHeight();
    }

    private void clampScroll(int totalItems) {
        int max = Math.max(0, totalItems - MAX_VISIBLE);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
    }

    private int getScrollbarX() {
        return x + width + 4;
    }

    private int getScrollbarHeight() {
        return MAX_VISIBLE * ROW_HEIGHT;
    }

    private int getThumbHeight(int totalItems) {
        int trackHeight = getScrollbarHeight();
        return Math.max(18, (MAX_VISIBLE * trackHeight) / totalItems);
    }

    private int getThumbY(int totalItems, int thumbHeight) {
        int trackHeight = getScrollbarHeight();
        int maxScroll = Math.max(1, totalItems - MAX_VISIBLE);
        int travel = Math.max(1, trackHeight - thumbHeight);
        return y + (scrollOffset * travel) / maxScroll;
    }

    public int getClickedIndex(double mouseX, double mouseY) {
        List<Integer> slots = rebuildNonEmptySlots();

        if (mouseX < x || mouseX > x + width) return -1;
        if (mouseY < y || mouseY > y + (MAX_VISIBLE * ROW_HEIGHT)) return -1;

        int row = (int) ((mouseY - y) / ROW_HEIGHT);
        int listIndex = scrollOffset + row;

        if (listIndex < 0 || listIndex >= slots.size()) return -1;
        return slots.get(listIndex);
    }

    public boolean clickedDrop(double mouseX) {
        return !isKeyList() && mouseX >= x && mouseX <= x + DROP_HIT_WIDTH;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + getScrollbarHeight();
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int offset) {
        scrollOffset = offset;
        clampScroll(rebuildNonEmptySlots().size());
    }
}
