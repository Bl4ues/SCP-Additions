package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079ChatLayout;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Fixed, preference-independent chat history for playable SCP-079. */
@Mixin(value = ChatComponent.class, priority = 3000)
public abstract class Scp079ChatComponentMixin {
    private static final int LINE_HEIGHT = 13;
    private static final int MESSAGE_PANEL_RGB = 0x071116;
    private static final int MESSAGE_EDGE_RGB = 0x5B8392;

    @Shadow @Final private Minecraft minecraft;
    // Use the same authoritative, already-wrapped list vanilla renders. The
    // previous allMessages pass could lag behind the visible list during local
    // SCP-079 echo insertion and left the terminal history apparently empty.
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$renderScp079Chat(GuiGraphics graphics,
            int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Scp079PlayableClient.active()) return;
        ci.cancel();

        boolean focused = this.minecraft.screen instanceof ChatScreen;
        if (this.trimmedMessages.isEmpty()) return;

        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int bottom = Scp079ChatLayout.historyBottom(
                this.minecraft.getWindow().getGuiScaledHeight());
        int width = Scp079ChatLayout.historyWidth(screenWidth);
        int row = 0;

        for (int index = 0;
                index < this.trimmedMessages.size()
                        && row < Scp079ChatLayout.HISTORY_LINES;
                index++) {
            GuiMessage.Line line = this.trimmedMessages.get(index);
            int age = Math.max(0, tickCount - line.addedTime());
            if (!focused && age >= Scp079ChatLayout.HISTORY_LIFETIME_TICKS) {
                continue;
            }

            float fade = focused ? 1.0F : fade(age);
            int textAlpha = Math.round(255.0F * fade);
            if (textAlpha <= 3) continue;

            int yBottom = bottom - row * LINE_HEIGHT;
            int yTop = yBottom - LINE_HEIGHT;
            int panelAlpha = Math.round(164.0F * fade);
            int edgeAlpha = Math.round(126.0F * fade);

            graphics.fill(Scp079ChatLayout.LEFT, yTop,
                    Scp079ChatLayout.LEFT + width, yBottom,
                    Scp079ChatLayout.withAlpha(MESSAGE_PANEL_RGB, panelAlpha));
            graphics.fill(Scp079ChatLayout.LEFT, yTop,
                    Scp079ChatLayout.LEFT + 2, yBottom,
                    Scp079ChatLayout.withAlpha(MESSAGE_EDGE_RGB, edgeAlpha));
            graphics.drawString(this.minecraft.font,
                    terminal(line.content()), Scp079ChatLayout.LEFT + 7,
                    yTop + 5,
                    (textAlpha << 24) | (Scp079UiTheme.TEXT & 0x00FFFFFF),
                    false);
            row++;
        }
    }

    private static FormattedCharSequence terminal(
            FormattedCharSequence content) {
        return sink -> content.accept((index, style, codePoint) ->
                sink.accept(index, style.withFont(ScpFonts.PF_VIDEOTEXT),
                        codePoint));
    }

    private static float fade(int age) {
        if (age <= Scp079ChatLayout.HISTORY_FADE_START_TICKS) return 1.0F;
        int fadeTicks = Math.max(1,
                Scp079ChatLayout.HISTORY_LIFETIME_TICKS
                        - Scp079ChatLayout.HISTORY_FADE_START_TICKS);
        return 1.0F - Mth.clamp(
                (age - Scp079ChatLayout.HISTORY_FADE_START_TICKS)
                        / (float) fadeTicks,
                0.0F, 1.0F);
    }
}
