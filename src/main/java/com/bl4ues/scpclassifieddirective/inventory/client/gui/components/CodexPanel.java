package com.bl4ues.scpclassifieddirective.inventory.client.gui.components;

import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.client.ClientInventoryBridge;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.item.CodexDocumentDefinition;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.network.DocumentActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CodexPanel {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final int WHITE = 0xFFB2B3B3, GRAY = 0xFF6A6C6C;
    private static final int BUTTON = 0x446A6C6C, BUTTON_HOVER = 0x667A7C7C;
    private static final int TRACK = 0x33000000, THUMB = 0x886A6C6C;
    private static final int BUTTON_H = 14, BAR_W = 5, LINE_H = 11;
    private static final double DRAG_THRESHOLD = 4.0D;
    private final ContextMenu contextMenu = new ContextMenu();
    private final int y, detailX, detailWidth, detailHeight, titleY, listTitleX, detailTitleX;
    private final List<ItemStack> documents;
    private final CodexListController list;
    private int selectedIndex = -1, textScrollOffset, contextIndex = -1, dragIndex = -1;
    private double dragX, dragY, lastX, lastY;
    private boolean showingText, expandedImage, dragMoved;
    private ItemStack dragged = ItemStack.EMPTY;
    private int expandedX, expandedY, expandedWidth, expandedHeight;

    public CodexPanel(int x, int y, int listWidth, int detailX, int detailWidth,
                      int titleY, int listTitleX, int detailTitleX, IScpInventory inventory) {
        this(x, y, listWidth, guessHeight(y), detailX, detailWidth, guessHeight(y),
                titleY, listTitleX, detailTitleX,
                inventory == null ? List.of() : inventory.getDocuments());
    }

    public CodexPanel(int x, int y, int listWidth, int listHeight, int detailX,
                      int detailWidth, int detailHeight, int titleY, int listTitleX,
                      int detailTitleX, List<ItemStack> documents) {
        this.y = y; this.detailX = detailX; this.detailWidth = detailWidth; this.detailHeight = detailHeight;
        this.titleY = titleY; this.listTitleX = listTitleX; this.detailTitleX = detailTitleX;
        this.documents = documents == null ? List.of() : documents;
        list = new CodexListController(x, y, listWidth, listHeight, this.documents);
    }

    public boolean isExpandedImage() { return expandedImage; }
    public void closeExpandedImage() { expandedImage = false; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getScrollOffset() { return list.scroll(); }
    public int getTextScrollOffset() { return textScrollOffset; }
    public boolean isShowingText() { return showingText; }

    public void setExpandedBounds(int x, int y, int width, int height) {
        expandedX = x;
        expandedY = y;
        expandedWidth = width;
        expandedHeight = height;
    }

    public void restoreSessionState(int selection, int scroll, int textScroll, boolean text) {
        selectedIndex = selection; list.scroll(scroll); textScrollOffset = textScroll;
        showingText = text; expandedImage = false; normalize(); clampText();
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        normalize();
        if (expandedImage && valid()) {
            renderExpanded(g);
            return;
        }

        title(g, listTitleX, "CLASSIFICATION");
        title(g, detailTitleX, "DOCUMENT");
        list.render(g, selectedIndex);
        renderDetails(g, mouseX, mouseY);
        contextMenu.render(g, mouseX, mouseY);
        if (dragMoved && !dragged.isEmpty()) {
            g.fill(mouseX - 12, mouseY - 12, mouseX + 12, mouseY + 12, 0x99303638);
            g.renderItem(dragged, mouseX - 8, mouseY - 8);
        }
    }

    private void renderExpanded(GuiGraphics g) {
        ItemStack stack = documents.get(selectedIndex);
        CodexDocumentDefinition definition =
                ScpItemClassifier.getCodexDefinitionOrFallback(stack);
        int sw = expandedWidth > 0 ? expandedWidth
                : MC.getWindow().getGuiScaledWidth();
        int sh = expandedHeight > 0 ? expandedHeight
                : MC.getWindow().getGuiScaledHeight();
        int x = expandedWidth > 0 ? expandedX : 0;
        int y = expandedHeight > 0 ? expandedY : 0;
        int mx = Math.max(18, sw / 16);
        int my = Math.max(12, sh / 24);

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 500.0F);
        g.fill(x, y, x + sw, y + sh, 0xFF000000);
        CodexDocumentView.renderPage(g, stack, definition,
                x + mx, y + my, sw - mx * 2, sh - my * 2);
        g.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (expandedImage) { expandedImage = false; return true; }
        if (contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option >= 0 && contextIndex >= 0) {
                ClientInventoryBridge.performDocument(contextIndex, contextMenu.getOption(option));
                contextMenu.close(); contextIndex = -1; return true;
            }
            contextMenu.close(); contextIndex = -1;
        }
        if (button == 0 && detailClick(mouseX, mouseY)) return true;
        CodexListController.Row row = list.click(mouseX, mouseY, button);
        if (row == null) return false;
        if (row.scrollbar() || row.category()) return button == 0;
        if (button == 1) {
            contextIndex = row.index(); contextMenu.open((int) mouseX, (int) mouseY, "Document"); return true;
        }
        if (button != 0) return false;
        selectedIndex = row.index(); showingText = expandedImage = false; textScrollOffset = 0;
        dragIndex = row.index(); dragged = documents.get(row.index()).copy();
        dragX = lastX = mouseX; dragY = lastY = mouseY; dragMoved = false; return true;
    }

    public int soundRegionAt(double mouseX, double mouseY) {
        if (expandedImage) return 1;
        if (contextMenu.isOpen()) {
            int option = contextMenu.clicked(mouseX, mouseY);
            if (option >= 0) return 10 + option;
        }
        if (valid()) {
            if (showingText && over(mouseX, mouseY, left(), controlY(),
                    58, BUTTON_H)) return 20;
            int gap = 6;
            int width = (right() - left() - gap) / 2;
            if (!showingText && over(mouseX, mouseY, left(), buttonY(),
                    width, BUTTON_H)) return 21;
            if (!showingText && over(mouseX, mouseY,
                    left() + width + gap, buttonY(), width, BUTTON_H)) {
                return 22;
            }
        }
        int listRegion = list.soundRegionAt(mouseX, mouseY);
        return listRegion == 0 ? 0 : 100 + listRegion;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignoredX, double ignoredY) {
        if (button != 0) return false;
        if (list.drag(mouseY)) return true;
        if (dragIndex < 0 || dragged.isEmpty()) return false;
        lastX = mouseX; lastY = mouseY;
        dragMoved |= Math.abs(mouseX - dragX) > DRAG_THRESHOLD || Math.abs(mouseY - dragY) > DRAG_THRESHOLD;
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button == 0 && dragIndex >= 0) {
            if (dragMoved && !inside(lastX, lastY))
                ClientInventoryBridge.performDocument(dragIndex, DocumentActionPacket.ACTION_DROP);
            dragIndex = -1; dragged = ItemStack.EMPTY; dragMoved = false; return true;
        }
        return button == 0 && list.release();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showingText && insideDetail(mouseX, mouseY) && valid()) {
            textScrollOffset += delta < 0 ? 3 : -3; clampText(); return true;
        }
        return list.wheel(mouseX, mouseY, delta);
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
        if (!valid()) return;
        ItemStack stack = documents.get(selectedIndex);
        CodexDocumentDefinition definition = ScpItemClassifier.getCodexDefinitionOrFallback(stack);
        if (showingText) renderText(g, stack, definition);
        else {
            int y2 = buttonY();
            int previewTop = Math.max(titleY + 14, top() - 18);
            CodexDocumentView.renderPage(g, stack, definition,
                    left() + 2, previewTop,
                    Math.max(40, right() - left() - 4),
                    Math.max(40, y2 - previewTop - 8));
            int gap = 6, width = (right() - left() - gap) / 2;
            button(g, left(), y2, width, "Show Document as Text", over(mouseX, mouseY, left(), y2, width, BUTTON_H));
            button(g, left() + width + gap, y2, width, "Expand Document",
                    over(mouseX, mouseY, left() + width + gap, y2, width, BUTTON_H));
        }
    }

    private void renderText(GuiGraphics g, ItemStack stack, CodexDocumentDefinition definition) {
        button(g, left(), controlY(), 58, "Return", false);
        int tx = left() + 2, ty = controlY() + BUTTON_H + 5;
        int width = Math.max(40, right() - BAR_W - 6 - tx), height = Math.max(40, bottom() - ty);
        String markdown = CodexDocumentView.text(stack, definition);
        int total = CodexDocumentView.lineCount(markdown, width), visible = Math.max(1, height / LINE_H);
        textScrollOffset = Math.max(0, Math.min(Math.max(0, total - visible), textScrollOffset));
        CodexDocumentView.renderMarkdown(g, markdown, tx, ty, width, height, textScrollOffset, LINE_H);
        textScrollbar(g, total, visible, right() - BAR_W, ty, height);
    }

    private boolean detailClick(double mouseX, double mouseY) {
        if (!valid()) return false;
        if (showingText && over(mouseX, mouseY, left(), controlY(), 58, BUTTON_H)) {
            showingText = false; textScrollOffset = 0; return true;
        }
        int gap = 6, width = (right() - left() - gap) / 2;
        if (!showingText && over(mouseX, mouseY, left(), buttonY(), width, BUTTON_H)) {
            showingText = true; textScrollOffset = 0; return true;
        }
        if (!showingText && over(mouseX, mouseY, left() + width + gap, buttonY(), width, BUTTON_H)) {
            expandedImage = true; return true;
        }
        return false;
    }

    private void normalize() {
        if (selectedIndex >= documents.size() || selectedIndex >= 0 && documents.get(selectedIndex).isEmpty()) {
            selectedIndex = -1; showingText = expandedImage = false; textScrollOffset = 0;
        }
    }

    private void clampText() {
        if (!valid()) { textScrollOffset = Math.max(0, textScrollOffset); return; }
        int ty = controlY() + BUTTON_H + 5, width = Math.max(40, right() - BAR_W - 8 - left());
        int visible = Math.max(1, Math.max(40, bottom() - ty) / LINE_H);
        ItemStack stack = documents.get(selectedIndex);
        int total = CodexDocumentView.lineCount(CodexDocumentView.text(stack,
                ScpItemClassifier.getCodexDefinitionOrFallback(stack)), width);
        textScrollOffset = Math.max(0, Math.min(Math.max(0, total - visible), textScrollOffset));
    }

    private void textScrollbar(GuiGraphics g, int total, int visible, int x, int y, int h) {
        if (total <= visible) return;
        int thumb = Math.max(18, h * visible / total);
        int thumbY = y + (h - thumb) * textScrollOffset / Math.max(1, total - visible);
        g.fill(x, y, x + BAR_W, y + h, TRACK); g.fill(x, thumbY, x + BAR_W, thumbY + thumb, THUMB);
    }

    private void button(GuiGraphics g, int x, int y, int width, String text, boolean hover) {
        g.fill(x, y, x + width, y + BUTTON_H, hover ? BUTTON_HOVER : BUTTON);
        Component label = ScpFonts.roboto(text);
        g.drawString(MC.font, label, x + (width - MC.font.width(label)) / 2, y + 3, WHITE, false);
    }

    private void title(GuiGraphics g, int x, String suffix) {
        Component prefix = ScpFonts.roboto("://CODEX_");
        g.drawString(MC.font, prefix, x, titleY, GRAY, false);
        g.drawString(MC.font, ScpFonts.roboto(suffix), x + MC.font.width(prefix), titleY, WHITE, false);
    }

    private boolean valid() { return selectedIndex >= 0 && selectedIndex < documents.size() && !documents.get(selectedIndex).isEmpty(); }
    private boolean insideDetail(double mx, double my) { return over(mx, my, left(), top(), right() - left(), bottom() - top()); }
    private boolean inside(double mx, double my) { return list.inside(mx, my) || insideDetail(mx, my); }
    private static boolean over(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    private int left() { return detailX + 4; }
    private int right() { return detailX + detailWidth - 4; }
    private int top() { return y; }
    private int bottom() { return y + Math.max(80, detailHeight) - 4; }
    private int buttonY() { return bottom() - BUTTON_H; }
    private int controlY() { return top() - 14; }
    private static int guessHeight(int y) { return Math.max(216, MC.getWindow().getGuiScaledHeight() - y - 160); }
}
