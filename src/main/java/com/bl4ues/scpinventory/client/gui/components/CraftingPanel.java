package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.ClientInventoryBridge;
import com.bl4ues.scpinventory.client.ScpCraftingClientState;
import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.crafting.ScpCraftingRecipeHelper;
import com.bl4ues.scpinventory.crafting.ScpCraftingState;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Portable 3x3 crafting UI backed by server-authoritative inventory actions. */
public final class CraftingPanel {
    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_DIM = 0xFF4A4C4C;
    private static final int ROW_BACKGROUND = 0x24303638;
    private static final int ROW_HOVER = 0x385A6365;
    private static final int ROW_SELECTED = 0x405F6D70;
    private static final int LINE_GRAY = 0x666A6C6C;
    private static final int SLOT_BACKGROUND = 0x66303638;
    private static final int SLOT_BORDER = 0xAA6A6C6C;
    private static final int SCROLL_TRACK = 0x44000000;
    private static final int SCROLL_THUMB = 0xAA6A6C6C;
    private static final int MISSING_RED = 0xAA9E4343;
    private static final int DIM_OVERLAY = 0x880C1011;

    private static final int RECIPE_PAD_X = 16;
    private static final int RECIPE_PAD_TOP = 18;
    private static final int RECIPE_ROW_HEIGHT = 46;
    private static final int RECIPE_ROW_GAP = 4;
    private static final int RECIPE_ICON_SIZE = 24;
    private static final int MATERIAL_ICON_SIZE = 16;
    private static final int GRID_SLOT_SIZE = 32;
    private static final int GRID_SLOT_GAP = 8;
    private static final int INVENTORY_ROW_HEIGHT = 32;
    private static final int INVENTORY_ICON_SIZE = 24;
    private static final int SCROLL_WIDTH = 5;
    private static final int PIN_WIDTH = 12;
    private static final int PIN_HEIGHT = 14;
    private static final int MAX_VISIBLE_INVENTORY_ROWS = 5;
    private static final double DRAG_THRESHOLD = 4.0D;
    private static final long MISSING_FLASH_MS = 950L;

    private enum DragKind { NONE, MAIN, GRID }

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

    private boolean requestedState;
    private int recipeScroll;
    private int inventoryScroll;
    private boolean recipeScrollbarDragging;
    private boolean inventoryScrollbarDragging;
    private ResourceLocation selectedRecipe;
    private ResourceLocation missingFlashRecipe;
    private long missingFlashUntil;

    private DragKind dragKind = DragKind.NONE;
    private int dragIndex = -1;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private double dragStartX;
    private double dragStartY;
    private boolean dragMoved;

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
        requestStateOnce();
        drawSectionTitle(graphics, recipesTitleX, titleY, "RECIPES");
        drawSectionTitle(graphics, gridTitleX, titleY, "GRID");
        IScpInventory inventory = getInventory();
        List<RecipeEntry> recipes = buildRecipeEntries(inventory);
        renderRecipes(graphics, recipes, mouseX, mouseY);
        renderGrid(graphics, mouseX, mouseY);
        renderCompactInventory(graphics, inventory, mouseX, mouseY);
        renderDraggedStack(graphics, mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInside(mouseX, mouseY, getRecipeListX(), getRecipeListY(),
                getRecipeListWidth(), getRecipeListHeight())) {
            int total = buildRecipeEntries(getInventory()).size();
            recipeScroll += delta < 0 ? 1 : -1;
            recipeScroll = clampScroll(recipeScroll, total,
                    getVisibleRecipeRows());
            return true;
        }
        if (isInside(mouseX, mouseY, getInventoryListX(), getInventoryListY(),
                getInventoryListWidth(), getInventoryListHeight())) {
            int total = getNonEmptyMainSlots(getInventory()).size();
            inventoryScroll += delta < 0 ? 1 : -1;
            inventoryScroll = clampScroll(inventoryScroll, total,
                    getVisibleInventoryRows());
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (clickRecipeScrollbar(mouseX, mouseY)) return true;
        if (clickInventoryScrollbar(mouseX, mouseY)) return true;

        List<RecipeEntry> recipes = buildRecipeEntries(getInventory());
        int recipeIndex = getClickedRecipeIndex(mouseX, mouseY, recipes.size());
        if (recipeIndex >= 0) {
            RecipeEntry entry = recipes.get(recipeIndex);
            if (isOverRecipePin(mouseX, mouseY, recipeIndex - recipeScroll)) {
                ClientInventoryBridge.togglePinnedCraftingRecipe(entry.id());
            } else {
                selectedRecipe = entry.id();
                if (entry.craftable()) {
                    missingFlashRecipe = null;
                    ClientInventoryBridge.autoFillCraftingRecipe(entry.id());
                } else {
                    missingFlashRecipe = entry.id();
                    missingFlashUntil = System.currentTimeMillis()
                            + MISSING_FLASH_MS;
                }
            }
            return true;
        }

        if (isInside(mouseX, mouseY, getOutputX(), getOutputY(),
                GRID_SLOT_SIZE, GRID_SLOT_SIZE)
                && !getCurrentResult().isEmpty()) {
            ClientInventoryBridge.craftPortableGrid();
            return true;
        }

        int gridSlot = getGridSlotAt(mouseX, mouseY);
        if (gridSlot >= 0) {
            ItemStack stack = getGridStack(gridSlot);
            if (!stack.isEmpty()) startDrag(DragKind.GRID, gridSlot, stack,
                    mouseX, mouseY);
            return true;
        }

        IScpInventory inventory = getInventory();
        int mainSlot = getMainSlotAt(mouseX, mouseY, inventory);
        if (mainSlot >= 0 && inventory != null) {
            ItemStack stack = inventory.getInventoryItem(mainSlot);
            if (!stack.isEmpty()) startDrag(DragKind.MAIN, mainSlot, stack,
                    mouseX, mouseY);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (button != 0) return false;
        if (recipeScrollbarDragging) {
            updateRecipeScrollFromMouse(mouseY);
            return true;
        }
        if (inventoryScrollbarDragging) {
            updateInventoryScrollFromMouse(mouseY);
            return true;
        }
        if (dragKind == DragKind.NONE) return false;
        if (Math.abs(mouseX - dragStartX) > DRAG_THRESHOLD
                || Math.abs(mouseY - dragStartY) > DRAG_THRESHOLD) {
            dragMoved = true;
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (recipeScrollbarDragging || inventoryScrollbarDragging) {
            recipeScrollbarDragging = false;
            inventoryScrollbarDragging = false;
            return true;
        }
        if (dragKind == DragKind.NONE) return false;
        if (dragMoved) finishDrag(mouseX, mouseY);
        clearDrag();
        return true;
    }

    private void renderRecipes(GuiGraphics graphics,
                               List<RecipeEntry> recipes,
                               int mouseX, int mouseY) {
        int visibleRows = getVisibleRecipeRows();
        recipeScroll = clampScroll(recipeScroll, recipes.size(), visibleRows);
        if (recipes.isEmpty()) {
            String text = "No learnt recipes";
            int textX = recipesX + (recipesWidth
                    - mc.font.width(ScpFonts.roboto(text))) / 2;
            int textY = recipesY + Math.max(42, recipesHeight / 2 - 6);
            graphics.drawString(mc.font, ScpFonts.roboto(text), textX, textY,
                    TEXT_GRAY, false);
            return;
        }

        for (int row = 0; row < visibleRows; row++) {
            int index = recipeScroll + row;
            if (index >= recipes.size()) break;
            RecipeEntry entry = recipes.get(index);
            int rowX = getRecipeListX();
            int rowY = getRecipeListY()
                    + row * (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP);
            boolean hovered = isInside(mouseX, mouseY, rowX, rowY,
                    getRecipeListWidth(), RECIPE_ROW_HEIGHT);
            boolean selected = entry.id().equals(selectedRecipe);
            graphics.fill(rowX, rowY, rowX + getRecipeListWidth(),
                    rowY + RECIPE_ROW_HEIGHT,
                    selected ? ROW_SELECTED : hovered ? ROW_HOVER : ROW_BACKGROUND);
            renderRecipeEntry(graphics, entry, rowX, rowY, mouseX, mouseY);
        }
        renderRecipeScrollbar(graphics, recipes.size(), visibleRows);
    }

    private void renderRecipeEntry(GuiGraphics graphics, RecipeEntry entry,
                                   int rowX, int rowY,
                                   int mouseX, int mouseY) {
        int iconX = rowX + 8;
        int iconY = rowY + 5;
        drawSlot(graphics, iconX, iconY, RECIPE_ICON_SIZE);
        graphics.renderItem(entry.output(), iconX + 4, iconY + 4);
        if (!entry.craftable()) {
            graphics.fill(iconX + 1, iconY + 1,
                    iconX + RECIPE_ICON_SIZE - 1,
                    iconY + RECIPE_ICON_SIZE - 1, DIM_OVERLAY);
        }

        int pinX = rowX + getRecipeListWidth() - PIN_WIDTH - 7;
        int pinY = rowY + 5;
        drawPin(graphics, pinX, pinY, entry.pinned(),
                isInside(mouseX, mouseY, pinX, pinY, PIN_WIDTH, PIN_HEIGHT));

        int textX = iconX + RECIPE_ICON_SIZE + 9;
        int textMax = Math.max(20, pinX - textX - 6);
        drawTrimmed(graphics, entry.name(), textX, rowY + 5,
                textMax, entry.craftable() ? TEXT_WHITE : TEXT_GRAY);

        int materialX = textX;
        int materialY = rowY + 23;
        int materialRight = pinX - 3;
        boolean flash = entry.id().equals(missingFlashRecipe)
                && System.currentTimeMillis() < missingFlashUntil
                && ((missingFlashUntil - System.currentTimeMillis()) / 110L) % 2L == 0L;
        int hiddenGroups = 0;
        for (IngredientGroup group : entry.ingredients()) {
            int neededWidth = MATERIAL_ICON_SIZE + (group.count() > 1 ? 14 : 4);
            if (materialX + neededWidth > materialRight) {
                hiddenGroups++;
                continue;
            }
            graphics.renderItem(group.display(), materialX, materialY);
            if (!group.available()) {
                graphics.fill(materialX, materialY,
                        materialX + MATERIAL_ICON_SIZE,
                        materialY + MATERIAL_ICON_SIZE,
                        flash ? MISSING_RED : DIM_OVERLAY);
                if (flash) drawFrame(graphics, materialX, materialY,
                        MATERIAL_ICON_SIZE, MISSING_RED);
            }
            if (group.count() > 1) {
                graphics.drawString(mc.font,
                        ScpFonts.roboto("x" + group.count()),
                        materialX + MATERIAL_ICON_SIZE - 1, materialY + 7,
                        group.available() ? TEXT_WHITE : 0xFFA06F6F, false);
            }
            materialX += neededWidth + 4;
        }
        if (hiddenGroups > 0 && materialX < materialRight - 12) {
            graphics.drawString(mc.font,
                    ScpFonts.roboto("+" + hiddenGroups), materialX,
                    materialY + 6, TEXT_GRAY, false);
        }
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = getGridStartX();
        int startY = getGridStartY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = row * 3 + column;
                int slotX = startX + column * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                int slotY = startY + row * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                drawSlot(graphics, slotX, slotY, GRID_SLOT_SIZE);
                ItemStack stack = getGridStack(slot);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX + 8, slotY + 8);
                }
                if (isInside(mouseX, mouseY, slotX, slotY,
                        GRID_SLOT_SIZE, GRID_SLOT_SIZE)) {
                    graphics.fill(slotX + 1, slotY + 1,
                            slotX + GRID_SLOT_SIZE - 1,
                            slotY + GRID_SLOT_SIZE - 1, 0x225F7477);
                }
            }
        }

        int centerY = getOutputY() + GRID_SLOT_SIZE / 2;
        int arrowStart = startX + getGridPixels() + 12;
        int arrowEnd = getOutputX() - 8;
        graphics.fill(arrowStart, centerY - 1, arrowEnd, centerY + 1,
                LINE_GRAY);
        graphics.fill(arrowEnd - 5, centerY - 5, arrowEnd, centerY + 6,
                LINE_GRAY);

        drawSlot(graphics, getOutputX(), getOutputY(), GRID_SLOT_SIZE);
        ItemStack result = getCurrentResult();
        if (!result.isEmpty()) {
            graphics.renderItem(result, getOutputX() + 8, getOutputY() + 8);
            if (result.getCount() > 1) {
                graphics.renderItemDecorations(mc.font, result,
                        getOutputX() + 8, getOutputY() + 8);
            }
        }
    }

    private void renderCompactInventory(GuiGraphics graphics,
                                        IScpInventory inventory,
                                        int mouseX, int mouseY) {
        if (inventory == null) return;

        List<Integer> slots = getNonEmptyMainSlots(inventory);
        int visible = getVisibleInventoryRows();
        inventoryScroll = clampScroll(inventoryScroll, slots.size(), visible);
        if (slots.isEmpty()) {
            graphics.drawString(mc.font, ScpFonts.roboto("Inventory is empty"),
                    getInventoryListX(), getInventoryListY() + 10,
                    TEXT_GRAY, false);
            return;
        }

        for (int row = 0; row < visible; row++) {
            int listIndex = inventoryScroll + row;
            if (listIndex >= slots.size()) break;
            int mainSlot = slots.get(listIndex);
            ItemStack stack = inventory.getInventoryItem(mainSlot);
            int rowX = getInventoryListX();
            int rowY = getInventoryListY() + row * INVENTORY_ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, rowX, rowY,
                    getInventoryListWidth(), INVENTORY_ROW_HEIGHT);
            if (hovered) graphics.fill(rowX, rowY,
                    rowX + getInventoryListWidth(), rowY + INVENTORY_ROW_HEIGHT,
                    ROW_HOVER);
            drawSlot(graphics, rowX, rowY + 4, INVENTORY_ICON_SIZE);
            graphics.renderItem(stack, rowX + 4, rowY + 8);
            int textX = rowX + INVENTORY_ICON_SIZE + 10;
            int textWidth = Math.max(24,
                    getInventoryListWidth() - INVENTORY_ICON_SIZE - 22);
            drawTrimmed(graphics, stack.getHoverName().getString(),
                    textX, rowY + 6, textWidth, TEXT_WHITE);
            drawTrimmed(graphics, ScpItemClassifier.getDisplayType(stack),
                    textX, rowY + 19, textWidth, TEXT_GRAY);
            graphics.fill(rowX, rowY + INVENTORY_ROW_HEIGHT - 1,
                    rowX + getInventoryListWidth(),
                    rowY + INVENTORY_ROW_HEIGHT, LINE_GRAY);
        }
        renderInventoryScrollbar(graphics, slots.size(), visible);
    }

    private List<RecipeEntry> buildRecipeEntries(IScpInventory inventory) {
        if (mc.level == null) return List.of();
        Set<ResourceLocation> learnt = ScpCraftingClientState.getLearntRecipes();
        Set<ResourceLocation> pinned = ScpCraftingClientState.getPinnedRecipes();
        List<ItemStack> available = new ArrayList<>();
        if (inventory != null) available.addAll(
                ScpCraftingRecipeHelper.nonEmptyCopies(inventory.getInventory()));
        available.addAll(ScpCraftingRecipeHelper.nonEmptyCopies(
                ScpCraftingClientState.getGrid()));

        List<RecipeEntry> result = new ArrayList<>();
        for (ResourceLocation id : learnt) {
            Optional<CraftingRecipe> optional = ScpCraftingRecipeHelper
                    .getById(mc.level, id);
            if (optional.isEmpty()) continue;
            CraftingRecipe recipe = optional.get();
            ItemStack output = recipe.getResultItem(mc.level.registryAccess()).copy();
            if (output.isEmpty()) continue;
            List<Ingredient> layout = ScpCraftingRecipeHelper.layout(recipe);
            boolean craftable = ScpCraftingRecipeHelper.assignSources(
                    available, layout) != null;
            result.add(new RecipeEntry(id, recipe, output,
                    output.getHoverName().getString(), craftable,
                    pinned.contains(id), buildIngredientGroups(recipe, available)));
        }
        Comparator<RecipeEntry> alphabetical = Comparator.comparing(
                entry -> entry.name().toLowerCase(Locale.ROOT));
        result.sort(Comparator
                .comparingInt((RecipeEntry entry) -> entry.pinned()
                        ? 0 : entry.craftable() ? 1 : 2)
                .thenComparing(alphabetical));
        return result;
    }

    private List<IngredientGroup> buildIngredientGroups(CraftingRecipe recipe,
                                                        List<ItemStack> available) {
        Map<ResourceLocation, MutableIngredientGroup> groups = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            ItemStack display = ScpCraftingRecipeHelper.representative(ingredient);
            if (display.isEmpty()) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(display.getItem());
            MutableIngredientGroup group = groups.computeIfAbsent(key,
                    ignored -> new MutableIngredientGroup(display, ingredient));
            group.count++;
        }

        List<IngredientGroup> result = new ArrayList<>();
        for (MutableIngredientGroup group : groups.values()) {
            int availableCount = 0;
            for (ItemStack stack : available) {
                if (group.ingredient.test(stack)) availableCount++;
            }
            result.add(new IngredientGroup(group.display, group.count,
                    availableCount >= group.count));
        }
        return result;
    }

    private ItemStack getCurrentResult() {
        if (mc.level == null) return ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = ScpCraftingRecipeHelper.findMatching(
                mc.level, ScpCraftingClientState.getGrid());
        if (recipe.isEmpty()) return ItemStack.EMPTY;
        TransientCraftingContainer container = ScpCraftingRecipeHelper
                .createContainer(ScpCraftingClientState.getGrid());
        return recipe.get().assemble(container, mc.level.registryAccess());
    }

    private void finishDrag(double mouseX, double mouseY) {
        int targetGrid = getGridSlotAt(mouseX, mouseY);
        if (dragKind == DragKind.MAIN && targetGrid >= 0) {
            ClientInventoryBridge.moveMainToCraftingGrid(dragIndex, targetGrid);
        } else if (dragKind == DragKind.GRID && targetGrid >= 0) {
            ClientInventoryBridge.moveCraftingGridToGrid(dragIndex, targetGrid);
        } else if (dragKind == DragKind.GRID
                && isInside(mouseX, mouseY, getInventoryListX(),
                getInventoryListY(), getInventoryListWidth(),
                getInventoryListHeight())) {
            int targetMain = getMainSlotAt(mouseX, mouseY, getInventory());
            ClientInventoryBridge.moveCraftingGridToMain(dragIndex, targetMain);
        }
    }

    private void startDrag(DragKind kind, int index, ItemStack stack,
                           double mouseX, double mouseY) {
        dragKind = kind;
        dragIndex = index;
        draggedStack = stack.copy();
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragMoved = false;
    }

    private void clearDrag() {
        dragKind = DragKind.NONE;
        dragIndex = -1;
        draggedStack = ItemStack.EMPTY;
        dragMoved = false;
    }

    private void renderDraggedStack(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!dragMoved || draggedStack.isEmpty()) return;
        int x = mouseX - 12;
        int y = mouseY - 12;
        drawSlot(graphics, x, y, 24);
        graphics.renderItem(draggedStack, x + 4, y + 4);
    }

    private void requestStateOnce() {
        if (requestedState || mc.player == null || mc.level == null) return;
        requestedState = true;
        ClientInventoryBridge.requestCraftingState();
    }

    private IScpInventory getInventory() {
        if (mc.player == null) return null;
        return mc.player.getCapability(ScpInventoryCapability.INSTANCE)
                .resolve().orElse(null);
    }

    private ItemStack getGridStack(int slot) {
        List<ItemStack> grid = ScpCraftingClientState.getGrid();
        return slot >= 0 && slot < grid.size() ? grid.get(slot) : ItemStack.EMPTY;
    }

    private List<Integer> getNonEmptyMainSlots(IScpInventory inventory) {
        List<Integer> result = new ArrayList<>();
        if (inventory == null) return result;
        for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
            if (!inventory.getInventoryItem(slot).isEmpty()) result.add(slot);
        }
        return result;
    }

    private int getGridSlotAt(double mouseX, double mouseY) {
        int startX = getGridStartX();
        int startY = getGridStartY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int x = startX + column * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                int y = startY + row * (GRID_SLOT_SIZE + GRID_SLOT_GAP);
                if (isInside(mouseX, mouseY, x, y,
                        GRID_SLOT_SIZE, GRID_SLOT_SIZE)) return row * 3 + column;
            }
        }
        return -1;
    }

    private int getMainSlotAt(double mouseX, double mouseY,
                              IScpInventory inventory) {
        if (inventory == null || !isInside(mouseX, mouseY,
                getInventoryListX(), getInventoryListY(),
                getInventoryListWidth(), getInventoryListHeight())) return -1;
        int row = (int) ((mouseY - getInventoryListY()) / INVENTORY_ROW_HEIGHT);
        List<Integer> slots = getNonEmptyMainSlots(inventory);
        int index = inventoryScroll + row;
        return index >= 0 && index < slots.size() ? slots.get(index) : -1;
    }

    private int getClickedRecipeIndex(double mouseX, double mouseY,
                                      int totalRecipes) {
        if (!isInside(mouseX, mouseY, getRecipeListX(), getRecipeListY(),
                getRecipeListWidth(), getRecipeListHeight())) return -1;
        int row = (int) ((mouseY - getRecipeListY())
                / (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP));
        int index = recipeScroll + row;
        return index >= 0 && index < totalRecipes ? index : -1;
    }

    private boolean isOverRecipePin(double mouseX, double mouseY,
                                    int visibleRow) {
        if (visibleRow < 0 || visibleRow >= getVisibleRecipeRows()) return false;
        int rowY = getRecipeListY()
                + visibleRow * (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP);
        int pinX = getRecipeListX() + getRecipeListWidth() - PIN_WIDTH - 7;
        return isInside(mouseX, mouseY, pinX, rowY + 5,
                PIN_WIDTH, PIN_HEIGHT);
    }

    private boolean clickRecipeScrollbar(double mouseX, double mouseY) {
        int total = buildRecipeEntries(getInventory()).size();
        if (total <= getVisibleRecipeRows() || !isInside(mouseX, mouseY,
                getRecipeScrollbarX(), getRecipeListY(), SCROLL_WIDTH,
                getRecipeListHeight())) return false;
        recipeScrollbarDragging = true;
        updateRecipeScrollFromMouse(mouseY);
        return true;
    }

    private boolean clickInventoryScrollbar(double mouseX, double mouseY) {
        int total = getNonEmptyMainSlots(getInventory()).size();
        if (total <= getVisibleInventoryRows() || !isInside(mouseX, mouseY,
                getInventoryScrollbarX(), getInventoryListY(), SCROLL_WIDTH,
                getInventoryListHeight())) return false;
        inventoryScrollbarDragging = true;
        updateInventoryScrollFromMouse(mouseY);
        return true;
    }

    private void updateRecipeScrollFromMouse(double mouseY) {
        int total = buildRecipeEntries(getInventory()).size();
        recipeScroll = scrollFromMouse(mouseY, getRecipeListY(),
                getRecipeListHeight(), total, getVisibleRecipeRows());
    }

    private void updateInventoryScrollFromMouse(double mouseY) {
        int total = getNonEmptyMainSlots(getInventory()).size();
        inventoryScroll = scrollFromMouse(mouseY, getInventoryListY(),
                getInventoryListHeight(), total, getVisibleInventoryRows());
    }

    private int scrollFromMouse(double mouseY, int trackY, int trackHeight,
                                int total, int visible) {
        int maxScroll = Math.max(0, total - visible);
        if (maxScroll == 0) return 0;
        double ratio = (mouseY - trackY) / Math.max(1.0D, trackHeight);
        return Math.max(0, Math.min(maxScroll,
                (int) Math.round(ratio * maxScroll)));
    }

    private void renderRecipeScrollbar(GuiGraphics graphics, int total,
                                       int visible) {
        renderScrollbar(graphics, getRecipeScrollbarX(), getRecipeListY(),
                getRecipeListHeight(), total, visible, recipeScroll);
    }

    private void renderInventoryScrollbar(GuiGraphics graphics, int total,
                                          int visible) {
        renderScrollbar(graphics, getInventoryScrollbarX(),
                getInventoryListY(), getInventoryListHeight(), total,
                visible, inventoryScroll);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y,
                                 int height, int total, int visible,
                                 int scroll) {
        if (total <= visible || visible <= 0) return;
        int thumbHeight = Math.max(18,
                Math.round(height * (visible / (float) total)));
        int travel = Math.max(1, height - thumbHeight);
        int maxScroll = Math.max(1, total - visible);
        int thumbY = y + Math.round(travel * (scroll / (float) maxScroll));
        graphics.fill(x, y, x + SCROLL_WIDTH, y + height, SCROLL_TRACK);
        graphics.fill(x, thumbY, x + SCROLL_WIDTH,
                thumbY + thumbHeight, SCROLL_THUMB);
    }

    private void drawPin(GuiGraphics graphics, int x, int y,
                         boolean pinned, boolean hovered) {
        int color = pinned ? TEXT_WHITE : hovered ? 0xFF909292 : TEXT_GRAY;
        graphics.fill(x + 3, y + 1, x + 9, y + 3, color);
        graphics.fill(x + 5, y + 3, x + 7, y + 7, color);
        graphics.fill(x + 2, y + 6, x + 10, y + 8, color);
        graphics.fill(x + 5, y + 8, x + 7, y + 12, color);
        graphics.fill(x + 6, y + 11, x + 7, y + 14, color);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, SLOT_BACKGROUND);
        drawFrame(graphics, x, y, size, SLOT_BORDER);
    }

    private void drawFrame(GuiGraphics graphics, int x, int y, int size,
                           int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }

    private void drawTrimmed(GuiGraphics graphics, String text, int x, int y,
                             int maxWidth, int color) {
        if (text == null || maxWidth <= 0) return;
        String value = text;
        if (mc.font.width(ScpFonts.roboto(value)) > maxWidth) {
            int ellipsis = mc.font.width(ScpFonts.roboto("..."));
            value = mc.font.plainSubstrByWidth(value,
                    Math.max(0, maxWidth - ellipsis)).trim() + "...";
        }
        graphics.drawString(mc.font, ScpFonts.roboto(value), x, y,
                color, false);
    }

    private void drawSectionTitle(GuiGraphics graphics, int x, int y,
                                  String section) {
        String prefix = "://CRAFTING_";
        graphics.drawString(mc.font, ScpFonts.roboto(prefix), x, y,
                TEXT_GRAY, false);
        graphics.drawString(mc.font, ScpFonts.roboto(section),
                x + mc.font.width(ScpFonts.roboto(prefix)), y,
                TEXT_WHITE, false);
    }

    private int getRecipeListX() { return recipesX + RECIPE_PAD_X; }
    private int getRecipeListY() { return recipesY + RECIPE_PAD_TOP; }
    private int getRecipeListWidth() { return Math.max(90, recipesWidth - RECIPE_PAD_X * 2 - 9); }
    private int getRecipeListHeight() { return Math.max(RECIPE_ROW_HEIGHT, recipesHeight - RECIPE_PAD_TOP - 12); }
    private int getVisibleRecipeRows() { return Math.max(1, getRecipeListHeight() / (RECIPE_ROW_HEIGHT + RECIPE_ROW_GAP)); }
    private int getRecipeScrollbarX() { return recipesX + recipesWidth - 8; }

    private int getGridPixels() { return GRID_SLOT_SIZE * 3 + GRID_SLOT_GAP * 2; }
    private int getGridStartX() {
        int total = getGridPixels() + 44 + GRID_SLOT_SIZE;
        return gridX + Math.max(12, (gridWidth - total) / 2);
    }
    private int getGridStartY() { return gridY + 28; }
    private int getOutputX() { return getGridStartX() + getGridPixels() + 44; }
    private int getOutputY() { return getGridStartY() + (getGridPixels() - GRID_SLOT_SIZE) / 2; }

    private int getInventoryListX() { return gridX + 18; }
    private int getInventoryListY() { return getGridStartY() + getGridPixels() + 12; }
    private int getInventoryListWidth() { return Math.max(90, gridWidth - 45); }
    private int getInventoryListHeight() {
        int available = Math.max(INVENTORY_ROW_HEIGHT,
                gridY + gridHeight - getInventoryListY() - 10);
        return Math.min(INVENTORY_ROW_HEIGHT * MAX_VISIBLE_INVENTORY_ROWS, available);
    }
    private int getVisibleInventoryRows() {
        return Math.max(1, Math.min(MAX_VISIBLE_INVENTORY_ROWS,
                getInventoryListHeight() / INVENTORY_ROW_HEIGHT));
    }
    private int getInventoryScrollbarX() { return gridX + gridWidth - 12; }

    private static int clampScroll(int scroll, int total, int visible) {
        return Math.max(0, Math.min(Math.max(0, total - visible), scroll));
    }

    private static boolean isInside(double mouseX, double mouseY,
                                    int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private record RecipeEntry(ResourceLocation id, CraftingRecipe recipe,
                               ItemStack output, String name,
                               boolean craftable, boolean pinned,
                               List<IngredientGroup> ingredients) {
    }

    private record IngredientGroup(ItemStack display, int count,
                                   boolean available) {
    }

    private static final class MutableIngredientGroup {
        private final ItemStack display;
        private final Ingredient ingredient;
        private int count;

        private MutableIngredientGroup(ItemStack display,
                                       Ingredient ingredient) {
            this.display = display;
            this.ingredient = ingredient;
        }
    }
}
