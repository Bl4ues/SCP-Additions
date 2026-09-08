package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Dedicated history backing for playable SCP-079's terminal chat.
 *
 * Vanilla ChatComponent's wrapped-line cache is intentionally bypassed here.
 * SCP-079 speech never enters global chat, so tying its visible history to that
 * cache made local speech disappear depending on which chat path populated it.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class Scp079TerminalChatHistoryClient {
    private static final int MAX_MESSAGES = 40;
    private static final int LINE_HEIGHT = 13;
    private static final int MESSAGE_PANEL_RGB = 0x071116;
    private static final int MESSAGE_EDGE_RGB = 0x5B8392;
    private static final long TICK_NANOS = 50_000_000L;
    private static final long DEDUPE_NANOS = 75_000_000L;

    private static final Deque<Entry> MESSAGES = new ArrayDeque<>();
    private static boolean lastActive;
    private static String lastRecorded = "";
    private static long lastRecordedAt;

    private Scp079TerminalChatHistoryClient() { }

    public static void record(Component message) {
        if (message == null || !Scp079PlayableClient.active()) return;
        syncSession();
        String plain = message.getString();
        if (plain.isBlank()) return;
        long now = System.nanoTime();
        if (plain.equals(lastRecorded) && now - lastRecordedAt <= DEDUPE_NANOS) {
            return;
        }
        lastRecorded = plain;
        lastRecordedAt = now;
        MESSAGES.addFirst(new Entry(message.copy(), now));
        while (MESSAGES.size() > MAX_MESSAGES) MESSAGES.removeLast();
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft,
            boolean focused) {
        if (graphics == null || minecraft == null
                || !Scp079PlayableClient.active()) return;
        syncSession();
        if (MESSAGES.isEmpty()) return;

        int width = Scp079ChatLayout.historyWidth(
                minecraft.getWindow().getGuiScaledWidth());
        int contentWidth = Math.max(24, width - 14);
        int bottom = Scp079ChatLayout.historyBottom(
                minecraft.getWindow().getGuiScaledHeight());
        int row = 0;
        long now = System.nanoTime();

        for (Entry entry : MESSAGES) {
            float fade = focused ? 1.0F : fade(now - entry.createdAt);
            if (fade <= 0.015F) continue;
            List<FormattedCharSequence> wrapped = new ArrayList<>(
                    minecraft.font.split(
                            Scp079ChatLayout.terminalText(entry.message),
                            contentWidth));
            for (int lineIndex = wrapped.size() - 1;
                    lineIndex >= 0 && row < Scp079ChatLayout.HISTORY_LINES;
                    lineIndex--) {
                int yBottom = bottom - row * LINE_HEIGHT;
                int yTop = yBottom - LINE_HEIGHT;
                int textAlpha = Math.round(255.0F * fade);
                int panelAlpha = Math.round(164.0F * fade);
                int edgeAlpha = Math.round(126.0F * fade);

                graphics.fill(Scp079ChatLayout.LEFT, yTop,
                        Scp079ChatLayout.LEFT + width, yBottom,
                        Scp079ChatLayout.withAlpha(MESSAGE_PANEL_RGB,
                                panelAlpha));
                graphics.fill(Scp079ChatLayout.LEFT, yTop,
                        Scp079ChatLayout.LEFT + 2, yBottom,
                        Scp079ChatLayout.withAlpha(MESSAGE_EDGE_RGB,
                                edgeAlpha));
                graphics.drawString(minecraft.font, wrapped.get(lineIndex),
                        Scp079ChatLayout.LEFT + 7, yTop + 5,
                        (textAlpha << 24)
                                | (Scp079UiTheme.TEXT & 0x00FFFFFF),
                        false);
                row++;
            }
            if (row >= Scp079ChatLayout.HISTORY_LINES) break;
        }
    }

    public static void clear() {
        MESSAGES.clear();
        lastRecorded = "";
        lastRecordedAt = 0L;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        syncSession();
    }

    private static void syncSession() {
        boolean active = Scp079PlayableClient.active();
        if (active != lastActive) {
            clear();
            lastActive = active;
        }
    }

    private static float fade(long ageNanos) {
        long fadeStart = Scp079ChatLayout.HISTORY_FADE_START_TICKS * TICK_NANOS;
        long lifetime = Scp079ChatLayout.HISTORY_LIFETIME_TICKS * TICK_NANOS;
        if (ageNanos <= fadeStart) return 1.0F;
        if (ageNanos >= lifetime) return 0.0F;
        return 1.0F - Mth.clamp((ageNanos - fadeStart)
                / (float) Math.max(1L, lifetime - fadeStart), 0.0F, 1.0F);
    }

    private record Entry(Component message, long createdAt) { }
}
