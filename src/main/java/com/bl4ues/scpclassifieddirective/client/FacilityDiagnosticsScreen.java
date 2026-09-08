package com.bl4ues.scpclassifieddirective.client;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.FacilityDiagnosticsPacket;
import com.bl4ues.scpclassifieddirective.network.FacilityDiagnosticsResetPacket;

/**
 * Input controller for the physical ARC-Site-48 SCiPNET diagnostic terminal.
 * The interface is rendered persistently on the placed CRT by its block entity
 * renderer; this Screen only owns focus, cursor mapping, and button input.
 */
public final class FacilityDiagnosticsScreen extends Screen {
    public static final int TEX_W = 540;
    public static final int TEX_H = 396;
    public static final int ACTION_X = 328;
    public static final int ACTION_Y = 200;
    public static final int ACTION_WIDTH = 190;
    public static final int ACTION_HEIGHT = 24;

    private FacilityDiagnosticsPacket data;
    private long snapshotReceivedAtMillis;
    private double guiScale = 1.0D;
    private int leftPos;
    private int topPos;
    private boolean clickVariant;
    private boolean hoveringPrimaryAction;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(ScpFonts.montserrat("ARC-Site-48 SCiPNET Diagnostics"));
        this.data = data;
        this.snapshotReceivedAtMillis = Util.getMillis();
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FacilityDiagnosticsScreen current
                && current.isFor(data.terminalPos())) {
            current.applySnapshot(data);
            return;
        }
        minecraft.setScreen(new FacilityDiagnosticsScreen(data));
    }

    private void applySnapshot(FacilityDiagnosticsPacket snapshot) {
        this.data = snapshot;
        this.snapshotReceivedAtMillis = Util.getMillis();
    }

    @Override
    protected void init() {
        super.init();
        updateLayout();
        TeslaTerminalFocusClient.beginDiagnostic(data.terminalPos());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        updateLayout();
        double tx = textureX(mouseX);
        double ty = textureY(mouseY);
        hoveringPrimaryAction = TeslaTerminalFocusClient.inputReady()
                && actionEnabled() && inside(tx, ty, ACTION_X, ACTION_Y,
                ACTION_WIDTH, ACTION_HEIGHT);
        // Deliberately no 2D panel. The ordinary Screen cursor addresses the
        // physical CRT while the world remains visible around the computer.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            onClose();
            return true;
        }
        if (button != 0 || !TeslaTerminalFocusClient.inputReady()) {
            return true;
        }

        if (data.auxiliaryPowerOnline()) {
            playRandomClick();
        }
        double tx = textureX(mouseX);
        double ty = textureY(mouseY);
        if (actionEnabled() && inside(tx, ty, ACTION_X, ACTION_Y,
                ACTION_WIDTH, ACTION_HEIGHT)) {
            playSelect();
            FacilityDiagnosticsResetPacket action = data.analysisComplete()
                    ? FacilityDiagnosticsResetPacket.purge(data.terminalPos())
                    : FacilityDiagnosticsResetPacket.analyze(data.terminalPos());
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(action);
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || minecraft != null
                && minecraft.options.keyInventory.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        hoveringPrimaryAction = false;
        if (TeslaTerminalFocusClient.diagnosticActiveFor(data.terminalPos())) {
            TeslaTerminalFocusClient.end();
        }
        super.removed();
    }

    public FacilityDiagnosticsPacket data() {
        return data;
    }

    public boolean isFor(BlockPos pos) {
        return pos != null && pos.equals(data.terminalPos());
    }

    public boolean hoveringPrimaryAction() {
        return hoveringPrimaryAction;
    }

    public boolean actionEnabled() {
        return data.auxiliaryPowerOnline() && cooldownRemainingTicks() <= 0;
    }

    public int cooldownRemainingTicks() {
        long elapsedMillis = Math.max(0L,
                Util.getMillis() - snapshotReceivedAtMillis);
        int elapsedTicks = (int) Math.min(Integer.MAX_VALUE,
                elapsedMillis / 50L);
        return Math.max(0, data.cachePurgeCooldownTicks() - elapsedTicks);
    }

    private void updateLayout() {
        double aspect = TEX_W / (double) TEX_H;
        double targetHeight = Math.min(
                height * TeslaTerminalFocusClient.projectedHeightFraction(),
                (width * 0.90D) / aspect);
        guiScale = Math.max(0.01D, targetHeight / TEX_H);
        leftPos = (int) Math.round((width - TEX_W * guiScale) / 2.0D);
        topPos = (int) Math.round((height - TEX_H * guiScale) / 2.0D);
    }

    private double textureX(double mouseX) {
        return (mouseX - leftPos) / guiScale;
    }

    private double textureY(double mouseY) {
        return (mouseY - topPos) / guiScale;
    }

    private void playRandomClick() {
        clickVariant = !clickVariant;
        String id = clickVariant ? "click_1" : "click_2";
        float pitch = 0.90F + (float) (Math.random() * 0.20D);
        playBlockSound(id, pitch, 0.2F);
    }

    private void playSelect() {
        playBlockSound("select", 1.0F, 1.0F);
    }

    private void playBlockSound(String soundId, float pitch, float volume) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation("scp_classified_directive", soundId));
        if (sound == null) return;
        BlockPos pos = data.terminalPos();
        level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, sound, SoundSource.BLOCKS,
                volume, pitch, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }
}
