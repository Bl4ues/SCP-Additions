package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.compat.SimpleVoiceChatPresence;
import net.mcreator.scpadditions.config.ui.ConfigCenterVisuals;

import java.util.ArrayList;
import java.util.List;

/** First-run, client-local voice setup for Simple Voice Chat and SCP-939 mimicry. */
public final class Scp939VoiceSetupScreen extends Screen {
    private static final int DEVICE_ROWS = 4;
    private static final int DEVICE_ROW_HEIGHT = 28;
    private static final int DEVICE_ROW_GAP = 5;

    private final Screen parent;
    private final List<Button> deviceButtons = new ArrayList<>();
    private List<String> devices = List.of("");
    private int deviceScroll;
    private String selectedDevice = "";
    private boolean allowMimicry;
    private Button mimicryButton;
    private Button continueButton;
    private int themeMouseX;
    private int themeMouseY;
    private boolean prepared;

    public Scp939VoiceSetupScreen(Screen parent) {
        super(Component.literal("Voice Communication Setup"));
        this.parent = parent;
        this.allowMimicry = Scp939VoiceClientPreferences.allowMimicry();
    }

    public static boolean shouldOpenAutomatically() {
        return SimpleVoiceChatPresence.installed()
                && !Scp939VoiceClientPreferences.setupComplete();
    }

    @Override
    protected void init() {
        if (!prepared) {
            ConfigCenterVisuals.prepare(parent);
            prepared = true;
        }

        devices = SimpleVoiceChatMicrophoneClient.devices();
        if (devices.isEmpty()) devices = List.of("");
        selectedDevice = SimpleVoiceChatMicrophoneClient.selectedDevice();
        if (!devices.contains(selectedDevice)) selectedDevice = "";
        deviceScroll = Math.max(0, Math.min(deviceScroll,
                Math.max(0, devices.size() - DEVICE_ROWS)));

        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
        int y = Math.max(12, (height - panelHeight) / 2);
        int rowWidth = panelWidth - 48;
        int rowStartY = y + 116;

        deviceButtons.clear();
        for (int row = 0; row < DEVICE_ROWS; row++) {
            final int visibleRow = row;
            Button button = Button.builder(Component.empty(), ignored ->
                    selectVisibleDevice(visibleRow))
                    .bounds(x + 24,
                            rowStartY + row * (DEVICE_ROW_HEIGHT + DEVICE_ROW_GAP),
                            rowWidth, DEVICE_ROW_HEIGHT)
                    .build();
            deviceButtons.add(addRenderableWidget(button));
        }

        mimicryButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
            allowMimicry = !allowMimicry;
            refreshButtonLabels();
        }).bounds(x + 24, y + panelHeight - 94,
                rowWidth, 30).build());

        continueButton = addRenderableWidget(Button.builder(
                ScpFonts.roboto("SAVE & CONTINUE"), ignored -> complete())
                .bounds(x + panelWidth - 174, y + panelHeight - 48,
                        150, 28).build());

        refreshButtonLabels();
    }

    private int panelWidth() {
        return Mth.clamp(width - 44, 440, 690);
    }

    private int panelHeight() {
        return Mth.clamp(height - 36, 390, 458);
    }

    private void selectVisibleDevice(int row) {
        int index = deviceScroll + row;
        if (index < 0 || index >= devices.size()) return;
        selectedDevice = devices.get(index);
        refreshButtonLabels();
    }

    private void refreshButtonLabels() {
        for (int row = 0; row < deviceButtons.size(); row++) {
            int index = deviceScroll + row;
            Button button = deviceButtons.get(row);
            if (index >= devices.size()) {
                button.visible = false;
                button.active = false;
                continue;
            }
            button.visible = true;
            button.active = true;
            String raw = devices.get(index);
            String name = SimpleVoiceChatMicrophoneClient.displayName(raw);
            button.setMessage(ScpFonts.roboto(
                    raw.equals(selectedDevice) ? "SELECTED  ·  " + name : name));
        }
        if (mimicryButton != null) {
            mimicryButton.setMessage(ScpFonts.roboto(
                    "SCP-939 VOICE MIMICRY: " + (allowMimicry ? "ON" : "OFF")));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        ConfigCenterVisuals.renderBackdrop(this, graphics,
                themeMouseX, themeMouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        themeMouseX = mouseX;
        themeMouseY = mouseY;
        renderBackground(graphics);

        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int x = ConfigCenterVisuals.contentLeft(width, panelWidth);
        int y = Math.max(12, (height - panelHeight) / 2);
        ConfigCenterVisuals.drawPanel(graphics, font, x, y, panelWidth,
                panelHeight, "VOICE COMMUNICATION");

        int slide = ConfigCenterVisuals.contentOffsetX();
        int textX = x + 24 + slide;
        int contentWidth = panelWidth - 48;
        int textColor = ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.TEXT);
        int muted = ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED);
        int accent = ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT_BRIGHT);

        graphics.drawString(font, ScpFonts.roboto("SIMPLE VOICE CHAT DETECTED"),
                textX, y + 45, accent, false);
        drawWrapped(graphics,
                "Choose the input device used by voice chat. SCP Additions reads the same live voice packets that Simple Voice Chat already sends; it does not open a second microphone stream.",
                textX, y + 61, contentWidth, muted, 12, 2);

        graphics.drawString(font, ScpFonts.roboto("INPUT DEVICE"),
                textX, y + 98, textColor, false);

        for (int row = 0; row < deviceButtons.size(); row++) {
            Button button = deviceButtons.get(row);
            if (!button.visible) continue;
            ConfigCenterVisuals.drawButton(graphics, font, button,
                    button.getMessage(), mouseX, mouseY);
            int index = deviceScroll + row;
            if (index >= 0 && index < devices.size()
                    && devices.get(index).equals(selectedDevice)) {
                int left = button.getX() + slide;
                graphics.fill(left + 4, button.getY() + 4,
                        left + 7, button.getY() + button.getHeight() - 4,
                        ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.GREEN));
            }
        }

        if (devices.size() > DEVICE_ROWS) {
            String browse = "Mouse wheel to browse microphones  ·  "
                    + (deviceScroll + 1) + "-"
                    + Math.min(devices.size(), deviceScroll + DEVICE_ROWS)
                    + " / " + devices.size();
            graphics.drawString(font, ScpFonts.roboto(browse), textX,
                    y + 116 + DEVICE_ROWS * (DEVICE_ROW_HEIGHT + DEVICE_ROW_GAP) + 1,
                    muted, false);
        }

        int privacyY = y + panelHeight - 157;
        graphics.drawString(font, ScpFonts.roboto("SCP-939 MIMICRY PERMISSION"),
                textX, privacyY, textColor, false);
        drawWrapped(graphics,
                "SCP-939 can still hear ordinary positional voice chat as sound. This permission only allows it to temporarily reuse short fragments of your voice. Fragments stay in memory, expire within two minutes, and are cleared on opt-out or disconnect; nothing is written to disk.",
                textX, privacyY + 16, contentWidth, muted, 12, 3);

        if (mimicryButton != null) {
            ConfigCenterVisuals.drawButton(graphics, font, mimicryButton,
                    mimicryButton.getMessage(), mouseX, mouseY);
        }
        if (continueButton != null) {
            ConfigCenterVisuals.drawButton(graphics, font, continueButton,
                    continueButton.getMessage(), mouseX, mouseY);
        }
    }

    private void drawWrapped(GuiGraphics graphics, String text, int x, int y,
            int width, int color, int lineStride, int maxLines) {
        int lineY = y;
        int count = 0;
        for (var line : font.split(ScpFonts.roboto(text), width)) {
            if (count++ >= maxLines) break;
            graphics.drawString(font, line, x, lineY, color, false);
            lineY += lineStride;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (devices.size() <= DEVICE_ROWS || delta == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int maximum = Math.max(0, devices.size() - DEVICE_ROWS);
        int next = Mth.clamp(deviceScroll + (delta < 0.0D ? 1 : -1),
                0, maximum);
        if (next == deviceScroll) return true;
        deviceScroll = next;
        refreshButtonLabels();
        return true;
    }

    private void complete() {
        SimpleVoiceChatMicrophoneClient.selectDevice(selectedDevice);
        Scp939VoiceClientPreferences.completeSetup(allowMimicry);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        // Closing the first-run screen is treated as a privacy-safe refusal rather
        // than asking again forever. The microphone choice is left untouched.
        Scp939VoiceClientPreferences.completeSetup(false);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
