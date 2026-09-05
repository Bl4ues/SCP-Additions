package com.bl4ues.scpclassifieddirective.inventory.client.gui.components;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.item.CodexDocumentDefinition;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class CodexListController {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int ROW = 24, BAR_W = 5;
    private static final int WHITE = 0xFFB2B3B3, GRAY = 0xFF6A6C6C, DARK = 0xFF202020;
    private static final int CATEGORY = 0x3E6A6C6C, SELECTED = 0x889FC8C8;
    private static final int TRACK = 0x33000000, THUMB = 0x886A6C6C;
    private final int x, y, width, height;
    private final List<ItemStack> documents;
    private final Set<String> collapsed = new HashSet<>();
    private int scroll;
    private boolean draggingBar;

    CodexListController(int x, int y, int width, int height, List<ItemStack> documents) {
        this.x = x; this.y = y; this.width = width; this.height = height; this.documents = documents;
    }

    int scroll() { return scroll; }
    void scroll(int value) { scroll = value; clamp(); }
    boolean draggingBar() { return draggingBar; }

    void render(GuiGraphics g, int selected) {
        List<Row> rows = rows(); clamp();
        for (int i = 0; i < visible() && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i); int rowY = contentY() + i * ROW;
            if (row.category()) {
                g.fill(x - 1, rowY, x + width - 22, rowY + ROW, CATEGORY);
                g.drawString(MC.font, ScpFonts.roboto(row.group()), x + 12, rowY + 7, WHITE, false);
                g.drawString(MC.font, collapsed.contains(row.group()) ? ">" : "v",
                        x + width - 44, rowY + 7, GRAY, false);
            } else {
                if (row.index() == selected) g.fill(x + 6, rowY, x + width - 36, rowY + ROW, SELECTED);
                g.drawString(MC.font, ScpFonts.roboto(row.name()), x + 26, rowY + 7,
                        row.index() == selected ? DARK : WHITE, false);
            }
        }
        scrollbar(g, rows.size());
    }

    Row click(double mouseX, double mouseY, int button) {
        if (button == 0 && over(mouseX, mouseY, barX() - 3, contentY(), BAR_W + 6, barHeight())
                && rows().size() > visible()) {
            draggingBar = true; moveBar(mouseY); return Row.bar();
        }
        if (!inside(mouseX, mouseY)) return null;
        int index = scroll + (int) ((mouseY - contentY()) / ROW);
        List<Row> rows = rows();
        if (index < 0 || index >= rows.size()) return null;
        Row row = rows.get(index);
        if (row.category() && button == 0) {
            if (!collapsed.remove(row.group())) collapsed.add(row.group());
            clamp();
        }
        return row;
    }

    boolean drag(double mouseY) {
        if (!draggingBar) return false;
        moveBar(mouseY); return true;
    }

    boolean release() {
        if (!draggingBar) return false;
        draggingBar = false; return true;
    }

    boolean wheel(double mouseX, double mouseY, double delta) {
        if (!inside(mouseX, mouseY)) return false;
        scroll += delta < 0 ? 1 : -1; clamp(); return true;
    }

    boolean inside(double mouseX, double mouseY) {
        return over(mouseX, mouseY, x - 1, contentY(), width - 15, height);
    }

    int soundRegionAt(double mouseX, double mouseY) {
        List<Row> rows = rows();
        if (rows.size() > visible()
                && over(mouseX, mouseY, barX() - 3, contentY(),
                BAR_W + 6, barHeight())) return 1;
        if (!inside(mouseX, mouseY)) return 0;
        int index = scroll + (int) ((mouseY - contentY()) / ROW);
        return index >= 0 && index < rows.size() ? 10 + index : 0;
    }

    private List<Row> rows() {
        Map<String, List<Row>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < documents.size(); i++) {
            ItemStack stack = documents.get(i); if (stack == null || stack.isEmpty()) continue;
            CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(stack);
            groups.computeIfAbsent(definition.getCategory(), ignored -> new ArrayList<>())
                    .add(Row.document(definition.getCategory(), i, definition.getDisplayName(stack)));
        }
        List<Row> result = new ArrayList<>();
        groups.forEach((group, entries) -> {
            entries.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
            result.add(Row.category(group));
            if (!collapsed.contains(group)) result.addAll(entries);
        });
        return result;
    }

    private void clamp() { scroll = Math.max(0, Math.min(Math.max(0, rows().size() - visible()), scroll)); }
    private int visible() { return Math.max(1, height / ROW); }
    private int contentY() { return y - 16; }
    private int barX() { return x + width - 14; }
    private int barHeight() { return visible() * ROW; }

    private void moveBar(double mouseY) {
        int total = rows().size(), visible = visible();
        if (total <= visible) { scroll = 0; return; }
        int height = barHeight(), thumb = Math.max(18, height * visible / total);
        scroll = (int) Math.round(((mouseY - contentY() - thumb / 2.0D)
                / Math.max(1, height - thumb)) * Math.max(1, total - visible));
        clamp();
    }

    private void scrollbar(GuiGraphics g, int total) {
        int visible = visible(); if (total <= visible) return;
        int h = barHeight(), thumb = Math.max(18, h * visible / total);
        int thumbY = contentY() + (h - thumb) * scroll / Math.max(1, total - visible);
        g.fill(barX(), contentY(), barX() + BAR_W, contentY() + h, TRACK);
        g.fill(barX(), thumbY, barX() + BAR_W, thumbY + thumb, THUMB);
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    record Row(boolean category, boolean scrollbar, String group, int index, String name) {
        static Row category(String group) { return new Row(true, false, group, -1, group); }
        static Row document(String group, int index, String name) { return new Row(false, false, group, index, name); }
        static Row bar() { return new Row(false, true, "", -1, ""); }
    }
}
