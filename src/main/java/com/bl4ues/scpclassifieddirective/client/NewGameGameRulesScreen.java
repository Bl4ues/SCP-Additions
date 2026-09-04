package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Modern, registry-driven editor for the game rules used by New Game. */
final class NewGameGameRulesScreen extends Screen {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int PANEL = 0xC20B0E12;
    private static final int ROW = 0xA80B0E12;
    private static final int BORDER = 0x70444C57;
    private static final int ERROR = 0xFFFF8B8B;
    private static final int ROW_HEIGHT = 36;
    private static final int GAP = 5;
    private static final int CATEGORY_HEIGHT = 30;

    private final Screen parent;
    private final WorldCreationUiState ui;
    private final GameRules working;
    private final List<RuleRow> rows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private NewGameWidgets.ActionButton cancel;
    private NewGameWidgets.ActionButton done;
    private int scroll;
    private int contentHeight;

    NewGameGameRulesScreen(Screen parent, WorldCreationUiState ui) {
        super(ScpFonts.montserrat("Game Rules"));
        this.parent = parent;
        this.ui = ui;
        this.working = ui.getGameRules().copy();
        collectRules();
    }

    @Override
    protected void init() {
        rowWidgets.clear();
        for (RuleRow row : rows) {
            AbstractWidget widget = row.createWidget(this.font);
            row.widget = widget;
            rowWidgets.add(widget);
            addRenderableWidget(widget);
        }
        cancel = addRenderableWidget(new NewGameWidgets.ActionButton(
                0, 0, 150, 34, ScpFonts.roboto("Back"), this::onClose));
        done = addRenderableWidget(new NewGameWidgets.ActionButton(
                0, 0, 180, 34, ScpFonts.roboto("Apply Rules"), this::apply));
        updateValidity();
    }

    private void collectRules() {
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key,
                    GameRules.Type<GameRules.BooleanValue> type) {
                if (!hidden(key)) rows.add(new BooleanRow(key));
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key,
                    GameRules.Type<GameRules.IntegerValue> type) {
                if (!hidden(key)) rows.add(new IntegerRow(key));
            }
        });
        rows.sort(Comparator
                .comparingInt((RuleRow row) -> row.category.ordinal())
                .thenComparing(row -> row.label.getString(),
                        String.CASE_INSENSITIVE_ORDER));
    }

    private static boolean hidden(GameRules.Key<?> key) {
        return key == ScpClassifiedDirectiveModGameRules.SCP079CONTROLON
                || key == ScpClassifiedDirectiveModGameRules.DECONCHECKPOINT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
        Layout layout = layout();
        float alpha = ConfigCenterVisuals.contentAlpha();

        graphics.fill(layout.x, layout.top, layout.x + layout.width,
                layout.bottom, fade(PANEL, alpha));
        graphics.fill(layout.x, layout.top, layout.x + 4,
                layout.bottom, fade(ACCENT, alpha));
        drawScaled(graphics, ScpFonts.montserrat("GAME RULES"),
                layout.x + 20, layout.top + 17, 1.28F, fade(TEXT, alpha));
        drawScaled(graphics, ScpFonts.titillium(
                        "Configure the rules applied when this world starts."),
                layout.x + 20, layout.top + 39, 0.98F, fade(MUTED, alpha));
        graphics.fill(layout.x + 20, layout.listTop - 10,
                layout.x + layout.width - 20, layout.listTop - 9,
                fade(BORDER, alpha));

        positionRows(layout);
        graphics.enableScissor(layout.x + 8, layout.listTop,
                layout.x + layout.width - 8, layout.listBottom);
        drawRows(graphics, layout, mouseX, mouseY, partialTick, alpha);
        graphics.disableScissor();

        drawScrollbar(graphics, layout, alpha);
        positionFooter(layout);
        cancel.render(graphics, mouseX, mouseY, partialTick);
        done.render(graphics, mouseX, mouseY, partialTick);
    }

    private void positionRows(Layout layout) {
        int y = layout.listTop - scroll;
        GameRules.Category previous = null;
        for (RuleRow row : rows) {
            if (row.category != previous) {
                y += CATEGORY_HEIGHT;
                previous = row.category;
            }
            row.y = y;
            if (row.widget != null) {
                int controlWidth = Mth.clamp(Math.round(layout.width * 0.31F),
                        150, 250);
                row.widget.setX(layout.x + layout.width - controlWidth - 24);
                row.widget.setY(y + 3);
                row.widget.setWidth(controlWidth);
                row.widget.setHeight(ROW_HEIGHT - 6);
                row.widget.visible = y + ROW_HEIGHT > layout.listTop
                        && y < layout.listBottom;
            }
            y += ROW_HEIGHT + GAP;
        }
        contentHeight = Math.max(0, y + scroll - layout.listTop);
        int max = Math.max(0, contentHeight
                - (layout.listBottom - layout.listTop));
        scroll = Mth.clamp(scroll, 0, max);
    }

    private void drawRows(GuiGraphics graphics, Layout layout, int mouseX,
            int mouseY, float partialTick, float alpha) {
        GameRules.Category previous = null;
        for (RuleRow row : rows) {
            if (row.category != previous) {
                int categoryY = row.y - CATEGORY_HEIGHT + 8;
                Component category = Component.translatable(
                        row.category.getDescriptionId());
                graphics.drawString(font, ScpFonts.montserrat(category),
                        layout.x + 24, categoryY,
                        fade(ACCENT_BRIGHT, alpha), false);
                previous = row.category;
            }
            if (row.y + ROW_HEIGHT <= layout.listTop
                    || row.y >= layout.listBottom) continue;

            boolean hovered = mouseX >= layout.x + 18
                    && mouseX < layout.x + layout.width - 18
                    && mouseY >= row.y && mouseY < row.y + ROW_HEIGHT;
            graphics.fill(layout.x + 18, row.y,
                    layout.x + layout.width - 18, row.y + ROW_HEIGHT,
                    fade(hovered ? 0xD5161B22 : ROW, alpha));
            graphics.fill(layout.x + 18, row.y,
                    layout.x + 21, row.y + ROW_HEIGHT,
                    fade(ACCENT, alpha));
            graphics.drawString(font, ScpFonts.roboto(row.label),
                    layout.x + 32,
                    row.y + (ROW_HEIGHT - font.lineHeight) / 2,
                    fade(hovered ? ACCENT_BRIGHT : TEXT, alpha), false);

            if (row.widget instanceof EditBox box) {
                drawEditSurface(graphics, box, row.valid, alpha);
            }
            if (row.widget != null && row.widget.visible) {
                row.widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void positionFooter(Layout layout) {
        int y = layout.bottom + 12;
        cancel.setX(layout.x);
        cancel.setY(y);
        done.setX(layout.x + layout.width - done.getWidth());
        done.setY(y);
    }

    private void drawScrollbar(GuiGraphics graphics, Layout layout,
            float alpha) {
        int viewport = layout.listBottom - layout.listTop;
        int max = Math.max(0, contentHeight - viewport);
        if (max <= 0) return;
        int x = layout.x + layout.width - 8;
        graphics.fill(x, layout.listTop, x + 2, layout.listBottom,
                fade(0x553A424D, alpha));
        int thumb = Math.max(18,
                Math.round(viewport * viewport / (float) contentHeight));
        int travel = viewport - thumb;
        int y = layout.listTop + Math.round(travel * (scroll / (float) max));
        graphics.fill(x, y, x + 2, y + thumb, fade(ACCENT, alpha));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        if (mouseX >= layout.x && mouseX < layout.x + layout.width
                && mouseY >= layout.listTop && mouseY < layout.listBottom) {
            int max = Math.max(0, contentHeight
                    - (layout.listBottom - layout.listTop));
            scroll = Mth.clamp(scroll + (delta > 0.0D ? -44 : 44), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private void apply() {
        updateValidity();
        if (!done.active) return;
        ui.setGameRules(working);
        Minecraft.getInstance().setScreen(parent);
    }

    private void updateValidity() {
        boolean valid = true;
        for (RuleRow row : rows) valid &= row.valid;
        if (done != null) done.active = valid;
    }

    private Layout layout() {
        int width = Mth.clamp(Math.round(this.width * 0.54F),
                Math.min(440, Math.max(300, this.width - 30)), 780);
        width = Math.min(width, Math.max(280, this.width - 28));
        int x = ConfigCenterVisuals.contentLeft(this.width, width);
        int top = Math.max(24, Math.round(this.height * 0.055F));
        int footerReserve = 62;
        int bottom = Math.max(top + 220, this.height - footerReserve);
        int listTop = top + 72;
        int listBottom = bottom - 16;
        return new Layout(x, width, top, bottom, listTop, listBottom);
    }

    private abstract class RuleRow {
        final GameRules.Category category;
        final Component label;
        AbstractWidget widget;
        int y;
        boolean valid = true;

        RuleRow(GameRules.Key<?> key) {
            this.category = key.getCategory();
            Component translated = Component.translatable(key.getDescriptionId());
            String resolved = translated.getString();
            this.label = resolved.equals(key.getDescriptionId())
                    ? Component.literal(humanize(key.getId())) : translated;
        }

        abstract AbstractWidget createWidget(Font font);
    }

    private final class BooleanRow extends RuleRow {
        private final GameRules.Key<GameRules.BooleanValue> key;

        BooleanRow(GameRules.Key<GameRules.BooleanValue> key) {
            super(key);
            this.key = key;
        }

        @Override
        AbstractWidget createWidget(Font font) {
            return new NewGameWidgets.Toggle(0, 0, 180, ROW_HEIGHT - 6,
                    "", working.getRule(key).get(), value -> {
                        working.getRule(key).set(value, null);
                        valid = true;
                        updateValidity();
                    });
        }
    }

    private final class IntegerRow extends RuleRow {
        private final GameRules.Key<GameRules.IntegerValue> key;

        IntegerRow(GameRules.Key<GameRules.IntegerValue> key) {
            super(key);
            this.key = key;
        }

        @Override
        AbstractWidget createWidget(Font font) {
            EditBox box = new EditBox(font, 0, 0, 180, ROW_HEIGHT - 6,
                    label);
            box.setBordered(false);
            box.setMaxLength(24);
            box.setTextColor(TEXT);
            box.setTextColorUneditable(MUTED);
            box.setValue(Integer.toString(working.getRule(key).get()));
            box.setResponder(value -> {
                valid = !value.isBlank()
                        && working.getRule(key).tryDeserialize(value);
                box.setTextColor(valid ? TEXT : ERROR);
                updateValidity();
            });
            return box;
        }
    }

    private static void drawEditSurface(GuiGraphics graphics, EditBox box,
            boolean valid, float alpha) {
        int x = box.getX();
        int y = box.getY();
        int w = box.getWidth();
        int h = box.getHeight();
        graphics.fill(x, y, x + w, y + h, fade(0xD00B0E12, alpha));
        int border = !valid ? ERROR : box.isFocused() ? ACCENT : BORDER;
        graphics.fill(x, y, x + w, y + 1, fade(border, alpha));
        graphics.fill(x, y + h - 1, x + w, y + h, fade(border, alpha));
        graphics.fill(x, y, x + 1, y + h, fade(border, alpha));
        graphics.fill(x + w - 1, y, x + w, y + h, fade(border, alpha));
    }

    private static String humanize(String id) {
        if (id == null || id.isBlank()) return "Game Rule";
        String spaced = id.replace('_', ' ').replace('-', ' ')
                .replaceAll("([a-z])([A-Z])", "$1 $2");
        StringBuilder out = new StringBuilder();
        for (String word : spaced.split("\\s+")) {
            if (!out.isEmpty()) out.append(' ');
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }

    private static void drawScaled(GuiGraphics graphics, Component text,
            float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, text,
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int fade(int color, float alpha) {
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * alpha), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    private record Layout(int x, int width, int top, int bottom,
            int listTop, int listBottom) {
    }
}
