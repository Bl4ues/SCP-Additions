package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Initial visual scaffold for the SCP Inventory crafting workflow. */
public final class CraftingPanel {
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int ROW_BACKGROUND = 0x24303638;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int SLOT_BACKGROUND = 0x66303638;
    private static final int SLOT_BORDER = 0xAA6A6C6C;

    private static final int RECIPE_PAD_X = 18;
    private static final int RECIPE_PAD_TOP = 18;
    private static final int RECIPE_ROW_HEIGHT = 40;
    private static final int RECIPE_ROW_GAP = 8;
    private static final int GRID_SLOT_SIZE = 32;
    private static final int GRID_SLOT_GAP = 8;

    private final Minecraft mc = Minecraft.getInstance();
    private final int recipesX;
    private final int recipesY;
    private final int recipesWidth;
    private final int recipesHeight;
    private final int gridX;
    private final int gridY;
    private final int gridWidth;
    private final int gridHeight;
    private final int titleY;
    private final int recipesTitleX;
    private final int gridTitleX;

    public CraftingPanel(int recipesX, int recipesY, int recipesWidth,
                         int recipesHeight, int gridX, int gridY,
                         int gridWidth, int gridHeight, int titleY,
                         int recipesTitleX, int gridTitleX) {
        this.recipesX = recipesX;
        this.recipesY = recipesY;
        this.recipesWidth = recipesWidth;
        this.recipesHeight = recipesHeight;
        this.gridX = gridX;
        this.gridY = gridY;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.titleY = titleY;
        this.recipesTitleX = recipesTitleX;
        this.gridTitleX = gridTitleX;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSectionTitle(graphics, recipesTitleX, titleY,
                "CRAFTING_RECIPES");
        drawSectionTitle(graphics, gridTitleX, titleY,
                "CRAFTING_GRID");
        renderRecipeScaffold(graphics);
        renderGridScaffold(graphics);
    }

    private void renderRecipeScaffold(GuiGraphics graphics) {
        int contentX = recipesX + RECIPE_PAD_X;
        int contentY = recipesY + RECIPE_PAD_TOP;
        int contentWidth = Math.max(80, recipesWidth - RECIPE_PAD_X * 2);
        int availableHeight = Math.max(0, recipesHeight - RECIPE_PAD_TOP - 12);
        int rows = Math.max(1, Math.min(6,
                availableHeight / (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP)));

        for (int row = 0; row < rows; row++) {
            int rowY = contentY + row * (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP);
            graphics.fill(contentX, rowY, contentX + contentWidth,
                    rowY + RECIPE_ROW_HEIGHT, ROW_BACKGROUND);
            drawEmptyCorners(graphics, contentX + 8, rowY + 8, 24);
            int lineX = contentX + 42;
            graphics.fill(lineX, rowY + 12,
                    contentX + contentWidth - 12, rowY + 13, LINE_GRAY);
            graphics.fill(lineX, rowY + 25,
                    contentX + Math.max(58, contentWidth * 2 / 3),
                    rowY + 26, LINE_GRAY);
        }
    }

    private void renderGridScaffold(GuiGraphics graphics) {
        int gridPixels = GRID_SLOT_SIZE * 3 + GRID_SLOT_GAP * 2;
        int outputGap = 42;
        int totalWidth = gridPixels + outputGap + GRID_SLOT_SIZE;
        int startX = gridX + Math.max(12, (gridWidth - totalWidth) / 2);
        int startY = gridY + Math.max(24,
                (gridHeight - gridPixels) / 2);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slotX = startX + column * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                int slotY = startY + row * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                drawSlot(graphics, slotX, slotY, GRID_SLOT_SIZE);
            }
        }

        int centerY = startY + (gridPixels - GRID_SLOT_SIZE) / 2;
        int arrowStart = startX + gridPixels + 10;
        int arrowEnd = arrowStart + 22;
        graphics.fill(arrowStart, centerY + GRID_SLOT_SIZE / 2 - 1,
                arrowEnd, centerY + GRID_SLOT_SIZE / 2 + 1, LINE_GRAY);
        graphics.fill(arrowEnd - 5, centerY + GRID_SLOT_SIZE / 2 - 5,
                arrowEnd, centerY + GRID_SLOT_SIZE / 2 + 6, LINE_GRAY);
        drawSlot(graphics, startX + gridPixels + outputGap,
                centerY, GRID_SLOT_SIZE);

        String placeholder = "CRAFTING SYSTEM READY";
        int textX = gridX + (gridWidth - mc.font.width(
                ScpFonts.roboto(placeholder))) / 2;
        int textY = Math.min(gridY + gridHeight - 22,
                startY + gridPixels + 24);
        graphics.drawString(mc.font, ScpFonts.roboto(placeholder),
                textX, textY, TEXT_GRAY, false);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, SLOT_BACKGROUND);
        graphics.fill(x, y, x + size, y + 1, SLOT_BORDER);
        graphics.fill(x, y + size - 1, x + size, y + size, SLOT_BORDER);
        graphics.fill(x, y, x + 1, y + size, SLOT_BORDER);
        graphics.fill(x + size - 1, y, x + size, y + size, SLOT_BORDER);
    }

    private void drawEmptyCorners(GuiGraphics graphics, int x, int y,
                                  int size) {
        int right = x + size;
        int bottom = y + size;
        int corner = 6;
        graphics.fill(x, y, x + corner, y + 1, SLOT_BORDER);
        graphics.fill(x, y, x + 1, y + corner, SLOT_BORDER);
        graphics.fill(right - corner, y, right, y + 1, SLOT_BORDER);
        graphics.fill(right - 1, y, right, y + corner, SLOT_BORDER);
        graphics.fill(x, bottom - 1, x + corner, bottom, SLOT_BORDER);
        graphics.fill(x, bottom - corner, x + 1, bottom, SLOT_BORDER);
        graphics.fill(right - corner, bottom - 1, right, bottom, SLOT_BORDER);
        graphics.fill(right - 1, bottom - corner, right, bottom, SLOT_BORDER);
    }

    private void drawSectionTitle(GuiGraphics graphics, int x, int y,
                                  String section) {
        String prefix = "://";
        graphics.drawString(mc.font, ScpFonts.roboto(prefix), x, y,
                TEXT_GRAY, false);
        graphics.drawString(mc.font, ScpFonts.roboto(section),
                x + mc.font.width(ScpFonts.roboto(prefix)), y,
                TEXT_WHITE, false);
    }
}
