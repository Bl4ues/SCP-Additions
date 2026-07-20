package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

/** Small RGB picker used by JSON-backed editors without external UI dependencies. */
public final class UnityColorPickerScreen extends Screen {
    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xF0141E42;
    private static final int BORDER = 0xFF46536C;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFFA9AFBA;

    private final Screen parent;
    private final Consumer<String> callback;
    private int red;
    private int green;
    private int blue;
    private EditBox hexBox;
    private ChannelSlider redSlider;
    private ChannelSlider greenSlider;
    private ChannelSlider blueSlider;
    private boolean syncing;

    public UnityColorPickerScreen(Screen parent, String initial, Consumer<String> callback) {
        super(ScpFonts.roboto("Choose a Color"));
        this.parent = parent;
        this.callback = callback;
        int rgb = parse(initial, 0xFFFFFF);
        this.red = rgb >> 16 & 255;
        this.green = rgb >> 8 & 255;
        this.blue = rgb & 255;
    }

    @Override
    protected void init() {
        int panelW = Math.min(430, width - 24);
        int left = (width - panelW) / 2;
        int top = Math.max(12, (height - 265) / 2);
        int x = left + 24;
        int sliderW = panelW - 48;

        redSlider = addRenderableWidget(new ChannelSlider(x, top + 62, sliderW, "Red", red,
                value -> { red = value; updateHexFromSliders(); }));
        greenSlider = addRenderableWidget(new ChannelSlider(x, top + 94, sliderW, "Green", green,
                value -> { green = value; updateHexFromSliders(); }));
        blueSlider = addRenderableWidget(new ChannelSlider(x, top + 126, sliderW, "Blue", blue,
                value -> { blue = value; updateHexFromSliders(); }));

        hexBox = new EditBox(font, x, top + 164, sliderW, 20, ScpFonts.roboto("Hex color"));
        hexBox.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
        hexBox.setMaxLength(7);
        hexBox.setValue(hex());
        hexBox.setResponder(this::updateSlidersFromHex);
        addRenderableWidget(hexBox);

        int buttonY = top + 205;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Apply"), button -> {
            if (callback != null) callback.accept(hex());
            Minecraft.getInstance().setScreen(parent);
        }).bounds(x, buttonY, (sliderW - 8) / 2, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"), button -> Minecraft.getInstance().setScreen(parent))
                .bounds(x + (sliderW - 8) / 2 + 8, buttonY, (sliderW - 8) / 2, 22).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelW = Math.min(430, width - 24);
        int left = (width - panelW) / 2;
        int top = Math.max(12, (height - 265) / 2);
        int right = left + panelW;
        int bottom = top + 250;

        graphics.fill(left, top, right, bottom, NAVY);
        graphics.fill(left, top, right, top + 34, NAVY_LIGHT);
        graphics.fill(left, top + 33, right, top + 34, ACCENT);
        drawBorder(graphics, left, top, panelW, bottom - top, BORDER);
        graphics.drawString(font, ScpFonts.roboto("SCP UNITY COLOR PICKER"), left + 16, top + 12, WHITE, false);

        int previewX = right - 74;
        int previewY = top + 43;
        graphics.fill(previewX, previewY, previewX + 50, previewY + 12, 0xFF000000 | rgb());
        drawBorder(graphics, previewX, previewY, 50, 12, BORDER);
        graphics.fill(previewX, previewY, previewX + 8, previewY + 1, ACCENT);
        graphics.drawString(font, ScpFonts.roboto("Preview"), left + 24, top + 46, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto("Hexadecimal and sliders stay synchronized."),
                left + 24, top + 188, MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateHexFromSliders() {
        if (hexBox == null || syncing) return;
        syncing = true;
        hexBox.setValue(hex());
        syncing = false;
    }

    private void updateSlidersFromHex(String value) {
        if (syncing || value == null || !value.matches("#[0-9A-Fa-f]{6}")) return;
        int rgb = parse(value, rgb());
        red = rgb >> 16 & 255;
        green = rgb >> 8 & 255;
        blue = rgb & 255;
        syncing = true;
        redSlider.setChannel(red);
        greenSlider.setChannel(green);
        blueSlider.setChannel(blue);
        syncing = false;
    }

    private int rgb() {
        return red << 16 | green << 8 | blue;
    }

    private String hex() {
        return String.format(Locale.ROOT, "#%06X", rgb());
    }

    private static int parse(String value, int fallback) {
        if (value == null) return fallback;
        String clean = value.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            return clean.matches("[0-9A-Fa-f]{6}") ? Integer.parseInt(clean, 16) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class ChannelSlider extends AbstractSliderButton {
        private final String label;
        private final Consumer<Integer> callback;

        private ChannelSlider(int x, int y, int width, String label, int channel, Consumer<Integer> callback) {
            super(x, y, width, 20, Component.empty(), channel / 255.0D);
            this.label = label;
            this.callback = callback;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(ScpFonts.roboto(label + ": " + channel()));
        }

        @Override
        protected void applyValue() {
            if (callback != null) callback.accept(channel());
        }

        private int channel() {
            return Math.max(0, Math.min(255, (int) Math.round(value * 255.0D)));
        }

        private void setChannel(int channel) {
            value = Math.max(0.0D, Math.min(1.0D, channel / 255.0D));
            updateMessage();
        }
    }
}
