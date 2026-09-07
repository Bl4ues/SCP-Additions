package com.bl4ues.scpclassifieddirective.mixin.client;

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
@Mixin(value = ChatComponent.class, priority = 2000)
public abstract class Scp079ChatComponentMixin {
    private static final int LINE_HEIGHT = 13;
    private static final int MESSAGE_PANEL_RGB = 0x071116;
    private static final int MESSAGE_EDGE_RGB = 0x5B8392;

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage> allMessages;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$renderScp079Chat(GuiGraphics graphics,
            int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Scp079PlayableClient.active()) return;
        ci.cancel();

        boolean focused = this.minecraft.screen instanceof ChatScreen;
        if (this.allMessages.isEmpty()) return;

        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int bottom = Scp079ChatLayout.historyBottom(
                this.minecraft.getWindow().getGuiScaledHeight());
        int width = Scp079ChatLayout.historyWidth(screenWidth);
        int maxLines = Scp079ChatLayout.HISTORY_LINES;
        int row = 0;

        for (int messageIndex = 0;
                messageIndex < this.allMessages.size() && row < maxLines;
                messageIndex++) {
            GuiMessage message = this.allMessages.get(messageIndex);
            int age = Math.max(0, tickCount - message.addedTime());
            if (!focused && age >= Scp079ChatLayout.HISTORY_LIFETIME_TICKS) {
                continue;
            }

            float fade = focused ? 1.0F : fade(age);
            int textAlpha = Math.round(255.0F * fade);
            if (textAlpha <= 3) continue;

            List<FormattedCharSequence> wrapped = this.minecraft.font.split(
                    Scp079ChatLayout.terminalText(message.content()),
                    Math.max(40, width - 14));
            for (int lineIndex = wrapped.size() - 1;
                    lineIndex >= 0 && row < maxLines; lineIndex--) {
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
                        wrapped.get(lineIndex), Scp079ChatLayout.LEFT + 7,
                        yTop + 3,
                        (textAlpha << 24) | (Scp079UiTheme.TEXT & 0x00FFFFFF),
                        false);
                row++;
            }
        }
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
