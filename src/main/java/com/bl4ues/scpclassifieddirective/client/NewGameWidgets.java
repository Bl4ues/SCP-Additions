package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Small modern controls shared by the custom New Game cards. */
final class NewGameWidgets {
    static final int TEXT = 0xFFF5F6F7;
    static final int MUTED = 0xFF9FA6AD;
    static final int ACCENT = 0xFFC99B18;
    static final int ACCENT_BRIGHT = 0xFFE3C865;
    static final int GREEN = 0xFF79D58B;
    static final int RED = 0xFFFF8B8B;
    static final int BASE = 0xB80B0E12;
    static final int HOVER = 0xEC161B22;
    static final int DISABLED = 0x9A11151A;
    static final int BORDER = 0x80444C57;

    private NewGameWidgets() {
    }

    static int fade(int color) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        int source = (color >>> 24) & 0xFF;
        int out = Mth.clamp(Math.round(source * alpha), 0, 255);
        return (out << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Borderless vanilla EditBox behavior pins text to its top-left corner.
     * The custom menus draw their own field surface, so keep the full vanilla
     * hitbox but translate only the text/caret into the visual inset.
     */
    static final class TextField extends EditBox {
        TextField(Font font, int x, int y, int width, int height,
                Component narration) {
            super(font, x, y, width, height, narration);
            setBordered(false);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int insetX = 10;
            int insetY = Math.max(0,
                    (getHeight() - Minecraft.getInstance().font.lineHeight) / 2);
            graphics.pose().pushPose();
            graphics.pose().translate(insetX, insetY, 0.0F);
            super.renderWidget(graphics, mouseX - insetX,
                    mouseY - insetY, partialTick);
            graphics.pose().popPose();
        }
    }

    static final class ActionButton extends AbstractWidget {
        private final Runnable action;
        private float hoverProgress;
        private long lastFrameAt = Util.getMillis();

        ActionButton(int x, int y, int width, int height,
                Component label, Runnable action) {
            super(x, y, width, height, label);
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            long now = Util.getMillis();
            float delta = Math.min(0.10F,
                    Math.max(0.0F, (now - lastFrameAt) / 1000.0F));
            lastFrameAt = now;
            boolean hovered = active && isMouseOver(mouseX, mouseY);
            hoverProgress = approach(hoverProgress, hovered ? 1.0F : 0.0F,
                    delta * 8.0F);
            float eased = smootherStep(hoverProgress);

            int background = !active ? DISABLED : blend(BASE, HOVER, eased);
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    fade(background));
            graphics.fill(getX(), getY(), getX() + Math.max(4,
                            Math.round(4.0F + eased * 2.0F)), getY() + height,
                    fade(active ? ACCENT : 0xFF555B64));
            int textColor = !active ? MUTED
                    : eased > 0.35F ? ACCENT_BRIGHT : TEXT;
            graphics.drawString(Minecraft.getInstance().font,
                    ScpFonts.roboto(getMessage()), getX() + 15,
                    getY() + Math.max(1, (height - 9) / 2),
                    fade(textColor), false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (active && action != null) action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class Toggle extends AbstractWidget {
        private boolean value;
        private final Consumer<Boolean> changed;
        private final String label;

        Toggle(int x, int y, int width, int height, String label,
                boolean value, Consumer<Boolean> changed) {
            super(x, y, width, height, Component.literal(label));
            this.label = label;
            this.value = value;
            this.changed = changed;
        }

        void setValue(boolean value) {
            this.value = value;
        }

        boolean value() {
            return value;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = active && isMouseOver(mouseX, mouseY);
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    fade(!active ? DISABLED : hovered ? HOVER : BASE));
            graphics.fill(getX(), getY(), getX() + 4, getY() + height,
                    fade(active ? ACCENT : 0xFF555B64));
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, ScpFonts.roboto(label),
                    getX() + 14, getY() + Math.max(1, (height - 9) / 2),
                    fade(active ? TEXT : MUTED), false);
            String state = value ? "ON" : "OFF";
            int sw = font.width(ScpFonts.roboto(state));
            graphics.drawString(font, ScpFonts.roboto(state),
                    getX() + width - 14 - sw,
                    getY() + Math.max(1, (height - 9) / 2),
                    fade(!active ? MUTED : value ? GREEN : RED), false);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!active) return;
            value = !value;
            if (changed != null) changed.accept(value);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    static final class Dropdown<T> extends AbstractWidget {
        record Entry<T>(T value, Component label, boolean enabled) {
            Entry(T value, String label) {
                this(value, Component.literal(label), true);
            }

            static <T> Entry<T> disabled(T value, String label) {
                return new Entry<>(value, Component.literal(label), false);
            }
        }

        private final List<Entry<T>> entries = new ArrayList<>();
        private final Consumer<T> changed;
        private final Function<T, Component> fallbackLabel;
        private T selected;
        private boolean open;
        private Component lockedLabel;

        Dropdown(int x, int y, int width, int height,
                List<Entry<T>> entries, T selected,
                Consumer<T> changed, Function<T, Component> fallbackLabel) {
            super(x, y, width, height, Component.empty());
            this.entries.addAll(entries);
            this.selected = selected;
            this.changed = changed;
            this.fallbackLabel = fallbackLabel;
        }

        void setEntries(List<Entry<T>> values) {
            entries.clear();
            entries.addAll(values);
            open = false;
        }

        void setSelected(T selected) {
            this.selected = selected;
        }

        T selected() {
            return selected;
        }

        void lock(Component label) {
            lockedLabel = label;
            open = false;
        }

        void unlock() {
            lockedLabel = null;
        }

        boolean isLocked() {
            return lockedLabel != null;
        }

        boolean isOpen() {
            return open && !isLocked() && visible;
        }

        int popupHeight() {
            return open ? entries.size() * height : 0;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = active && !isLocked() && isMouseOver(mouseX, mouseY);
            int background = !active || isLocked()
                    ? DISABLED : hovered ? HOVER : BASE;
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                    fade(background));
            graphics.fill(getX(), getY(), getX() + 4, getY() + height,
                    fade(active && !isLocked() ? ACCENT : 0xFF555B64));

            Component label = lockedLabel != null ? lockedLabel : selectedLabel();
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, ScpFonts.roboto(label),
                    getX() + 14, getY() + Math.max(1, (height - 9) / 2),
                    fade(active ? TEXT : MUTED), false);
            String arrow = isLocked() ? "LOCKED" : open ? "▲" : "▼";
            int aw = font.width(ScpFonts.roboto(arrow));
            graphics.drawString(font, ScpFonts.roboto(arrow),
                    getX() + width - 13 - aw,
                    getY() + Math.max(1, (height - 9) / 2),
                    fade(isLocked() ? MUTED : ACCENT_BRIGHT), false);
        }

        /** Draw open choices after the rest of the card so labels never bleed through. */
        void renderPopup(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            if (!isOpen()) return;
            var font = Minecraft.getInstance().font;
            int y = getY() + height + 2;
            for (int index = 0; index < entries.size(); index++) {
                Entry<T> entry = entries.get(index);
                int top = y + index * height;
                boolean optionHovered = entry.enabled()
                        && mouseX >= getX() && mouseX < getX() + width
                        && mouseY >= top && mouseY < top + height;
                int fill = !entry.enabled() ? 0xFC101317
                        : optionHovered ? 0xFF1A2028 : 0xFE0B0E12;
                graphics.fill(getX(), top, getX() + width, top + height,
                        fade(fill));
                graphics.fill(getX(), top, getX() + 4, top + height,
                        fade(entry.enabled() ? ACCENT : 0xFF4A5058));
                graphics.drawString(font, ScpFonts.roboto(entry.label()),
                        getX() + 14, top + Math.max(1, (height - 9) / 2),
                        fade(entry.enabled() ? TEXT : MUTED), false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !visible) return false;
            if (open && active && !isLocked()) {
                int y = getY() + height + 2;
                for (int index = 0; index < entries.size(); index++) {
                    int top = y + index * height;
                    if (mouseX >= getX() && mouseX < getX() + width
                            && mouseY >= top && mouseY < top + height) {
                        Entry<T> entry = entries.get(index);
                        if (entry.enabled()) {
                            selected = entry.value();
                            open = false;
                            if (changed != null) changed.accept(selected);
                        }
                        return true;
                    }
                }
            }
            if (isMouseOver(mouseX, mouseY)) {
                if (active && !isLocked()) open = !open;
                return true;
            }
            if (open) {
                open = false;
                return true;
            }
            return false;
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        private Component selectedLabel() {
            for (Entry<T> entry : entries) {
                if (Objects.equals(entry.value(), selected)) return entry.label();
            }
            return fallbackLabel == null ? Component.literal(String.valueOf(selected))
                    : fallbackLabel.apply(selected);
        }
    }

    private static int blend(int from, int to, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int a = Math.round(Mth.lerp(t, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF));
        int r = Math.round(Mth.lerp(t, (from >>> 16) & 0xFF, (to >>> 16) & 0xFF));
        int g = Math.round(Mth.lerp(t, (from >>> 8) & 0xFF, (to >>> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float approach(float value, float target, float amount) {
        if (value < target) return Math.min(target, value + amount);
        if (value > target) return Math.max(target, value - amount);
        return value;
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }
}
