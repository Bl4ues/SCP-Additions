package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.MainMenuSounds;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Gives the custom-drawn role selector the same hover/click language as other custom UI. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ScpRoleSelectorSoundsClient {
    private static ScpRoleSelectorScreen trackedScreen;
    private static int hoveredRegion;

    private ScpRoleSelectorSoundsClient() { }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ScpRoleSelectorScreen screen)) {
            trackedScreen = null;
            hoveredRegion = 0;
            return;
        }
        if (screen != trackedScreen) {
            trackedScreen = screen;
            hoveredRegion = 0;
        }
        int region = regionAt(screen, event.getMouseX(), event.getMouseY());
        if (region == hoveredRegion) return;
        hoveredRegion = region;
        if (region != 0 && MainMenuSounds.HOVER.isPresent()) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(MainMenuSounds.HOVER.get(), 1.0F));
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof ScpRoleSelectorScreen screen)
                || regionAt(screen, event.getMouseX(), event.getMouseY()) == 0
                || !ScpClassifiedDirectiveModSounds.SELECT.isPresent()) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        ScpClassifiedDirectiveModSounds.SELECT.get(),
                        1.0F, 0.35F));
    }

    private static int regionAt(ScpRoleSelectorScreen screen,
            double mouseX, double mouseY) {
        Layout layout = layout(screen.width, screen.height);
        int cardX = layout.x + 18;
        int cardY = layout.y + 108;
        int cardW = layout.leftW - 36;
        int cardH = Math.min(174, layout.h - 142);
        if (inside(mouseX, mouseY, cardX, cardY, cardW, cardH)) return 1;

        int confirmX = layout.x + layout.leftW + 22;
        int confirmY = layout.y + layout.h - 52;
        int confirmW = layout.w - layout.leftW - 42;
        if (inside(mouseX, mouseY, confirmX, confirmY, confirmW, 38)) return 2;

        int closeX = layout.x + layout.w - 35;
        int closeY = layout.y + 9;
        if (inside(mouseX, mouseY, closeX, closeY, 22, 22)) return 3;
        return 0;
    }

    private static Layout layout(int width, int height) {
        int w = Math.min(790, Math.max(560, width - 90));
        int h = Math.min(455, Math.max(340, height - 70));
        w = Math.min(w, width - 16);
        h = Math.min(h, height - 16);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int leftW = Math.min(205, Math.max(176, w * 25 / 100));
        return new Layout(x, y, w, h, leftW);
    }

    private static boolean inside(double mx, double my,
            int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private record Layout(int x, int y, int w, int h, int leftW) { }
}
