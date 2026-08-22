package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Frame-rate camera updates, direct drag handling and transition masking for the
 * death-screen live personnel feed.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeathSpectateUiEvents {
    private static boolean draggingFeed;

    private DeathSpectateUiEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ScpDeathScreen)
                || !MineZeroSpectateClient.active()) {
            draggingFeed = false;
            return;
        }
        MineZeroSpectateClient.updateForRender(event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ScpDeathScreen screen)
                || !MineZeroSpectateClient.active()) {
            return;
        }

        float cover = MineZeroSpectateClient.switchOcclusion();
        boolean lost = MineZeroSpectateClient.connectionLost();
        Bounds feed = feed(screen);
        var graphics = event.getGuiGraphics();

        if (cover > 0.001F || lost) {
            int a = Mth.clamp(Math.round(255.0F
                    * Mth.clamp(cover, 0.0F, 1.0F)), 0, 255);
            graphics.fill(feed.left, feed.top, feed.right, feed.bottom,
                    a << 24 | 0x00030406);

            long frame = Util.getMillis() / 38L;
            int strips = 18 + Math.round(cover * 34.0F);
            int feedHeight = Math.max(1, feed.bottom - feed.top);
            int feedWidth = Math.max(1, feed.right - feed.left);
            for (int i = 0; i < strips; i++) {
                long hash = noise(frame + i * 53L);
                int y = feed.top + Math.floorMod((int) hash, feedHeight);
                int height = 1 + Math.floorMod((int) (hash >>> 13), 7);
                int leftInset = Math.floorMod((int) (hash >>> 21),
                        Math.max(1, feedWidth / 5));
                int rightInset = Math.floorMod((int) (hash >>> 29),
                        Math.max(1, feedWidth / 6));
                int alpha = Mth.clamp(36 + Math.round(cover * 130.0F),
                        0, 190);
                int tint = ((hash >>> 39) & 3L) == 0L
                        ? 0x008B2028 : 0x00C3C9CC;
                graphics.fill(feed.left + leftInset, y,
                        Math.max(feed.left + leftInset + 1,
                                feed.right - rightInset),
                        Math.min(feed.bottom, y + height),
                        alpha << 24 | tint);
            }
        }

        if (lost) {
            Minecraft minecraft = Minecraft.getInstance();
            var title = ScpFonts.montserrat("CONNECTION LOST");
            var detail = ScpFonts.titillium("LIVE PERSONNEL FEED TERMINATED");
            int centerX = (feed.left + feed.right) / 2;
            int centerY = (feed.top + feed.bottom) / 2;
            graphics.drawString(minecraft.font, title,
                    centerX - minecraft.font.width(title) / 2,
                    centerY - 9, 0xFFE8E9EA, false);
            graphics.drawString(minecraft.font, detail,
                    centerX - minecraft.font.width(detail) / 2,
                    centerY + 9, 0xFFB55A60, false);
            return;
        }

        float voiceUiAlpha = Mth.clamp((0.72F - cover) / 0.42F,
                0.0F, 1.0F);
        SimpleVoiceChatDeathScreenUi.render(screen, graphics,
                event.getMouseX(), event.getMouseY(), voiceUiAlpha);
        Scp1576DeathScreenUi.render(screen, graphics,
                event.getMouseX(), event.getMouseY(), voiceUiAlpha);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof ScpDeathScreen screen)
                || !MineZeroSpectateClient.active()
                || MineZeroSpectateClient.connectionLost()) {
            return;
        }

        if (SimpleVoiceChatDeathScreenUi.handleMousePressed(screen,
                event.getMouseX(), event.getMouseY())) {
            draggingFeed = false;
            event.setCanceled(true);
            return;
        }

        if (!insideFeed(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        draggingFeed = true;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!draggingFeed || event.getMouseButton() != 0
                || !(event.getScreen() instanceof ScpDeathScreen)
                || !MineZeroSpectateClient.active()
                || MineZeroSpectateClient.connectionLost()) {
            return;
        }
        MineZeroSpectateClient.orbit(event.getDragX(), event.getDragY());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 0 || !draggingFeed) return;
        draggingFeed = false;
        if (event.getScreen() instanceof ScpDeathScreen
                && MineZeroSpectateClient.active()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        if (!(next instanceof ScpDeathScreen)) draggingFeed = false;
    }

    private static boolean insideFeed(ScpDeathScreen screen,
            double mouseX, double mouseY) {
        Bounds feed = feed(screen);
        return mouseX >= feed.left && mouseX < feed.right
                && mouseY >= feed.top && mouseY < feed.bottom;
    }

    private static Bounds feed(ScpDeathScreen screen) {
        int left = Math.max(screen.width / 2 + 28,
                Math.round(screen.width * 0.53F));
        int right = Math.max(left + 120, screen.width - 28);
        int top = Math.max(46, Math.round(screen.height * 0.095F));
        int bottom = Math.max(top + 100, screen.height - 64);
        return new Bounds(left, top, right, bottom);
    }

    private static long noise(long value) {
        long x = value + 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private record Bounds(int left, int top, int right, int bottom) {
    }
}
