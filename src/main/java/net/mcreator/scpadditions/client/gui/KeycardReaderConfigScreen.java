package net.mcreator.scpadditions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.KeycardReaderSetLevelPacket;

public final class KeycardReaderConfigScreen extends Screen {
    private static final int TEXTURE_WIDTH = 620;
    private static final int TEXTURE_HEIGHT = 100;
    private static final ResourceLocation BACKGROUND = texture("keycardgui.png");
    private static final ResourceLocation[] LEVEL_OVERLAYS = {
            texture("1.png"),
            texture("2.png"),
            texture("3.png"),
            texture("4.png"),
            texture("5.png"),
            texture("6.png")
    };

    private final BlockPos readerPos;
    private int selectedLevel;
    private boolean dragging;
    private double guiScale = 1.0D;
    private double left;
    private double top;

    private KeycardReaderConfigScreen(BlockPos readerPos, int initialLevel) {
        super(Component.translatable("screen.scp_additions.keycard_reader_config"));
        this.readerPos = readerPos.immutable();
        this.selectedLevel = Mth.clamp(initialLevel, 1, 6);
    }

    public static void open(BlockPos readerPos, int initialLevel) {
        Minecraft.getInstance().setScreen(new KeycardReaderConfigScreen(readerPos, initialLevel));
    }

    private static ResourceLocation texture(String file) {
        // These assets intentionally live directly in assets/scp_additions/gui.
        return new ResourceLocation("scp_additions", "gui/" + file);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        updateLayout();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) left, (float) top, 0.0F);
        guiGraphics.pose().scale((float) guiScale, (float) guiScale, 1.0F);

        guiGraphics.blit(BACKGROUND, 0, 0, 0.0F, 0.0F,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // The colored images are cumulative layers: level 3 renders 1, 2 and 3.
        for (int i = 0; i < selectedLevel; i++) {
            guiGraphics.blit(LEVEL_OVERLAYS[i], 0, 0, 0.0F, 0.0F,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && updateLevelFromMouse(mouseX, mouseY)) {
            dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging) {
            updateLevelFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            updateLevelFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean updateLevelFromMouse(double mouseX, double mouseY) {
        updateLayout();
        double localX = (mouseX - left) / guiScale;
        double localY = (mouseY - top) / guiScale;
        if (localX < 0.0D || localX > TEXTURE_WIDTH || localY < 0.0D || localY > TEXTURE_HEIGHT) {
            return false;
        }

        // Button spans: 0-100, 104-204, 208-308, 312-412,
        // 416-516 and 520-620. Assign each 4 px gap to the level on its left
        // so a continuous drag behaves like a volume slider.
        int level = Mth.clamp((int) (localX / 104.0D) + 1, 1, 6);
        if (level != selectedLevel) {
            selectedLevel = level;
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new KeycardReaderSetLevelPacket(readerPos, selectedLevel));
        }
        return true;
    }

    private void updateLayout() {
        guiScale = Math.min(1.0D,
                Math.min((width - 20.0D) / TEXTURE_WIDTH, (height - 20.0D) / TEXTURE_HEIGHT));
        guiScale = Math.max(guiScale, 0.1D);
        left = (width - TEXTURE_WIDTH * guiScale) / 2.0D;
        top = (height - TEXTURE_HEIGHT * guiScale) / 2.0D;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
