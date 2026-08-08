package net.mcreator.scpadditions.mixin.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.client.FacilityChatLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reuses Minecraft's chat history, scrolling, filtering and interaction model,
 * replacing only its presentation while the personal facility-chat toggle is on.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Unique private static final float SCP_ADDITIONS_MESSAGE_ENTER_TICKS = 7.0F;
    @Unique private static final float SCP_ADDITIONS_MESSAGE_SLIDE = 10.0F;

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    @Shadow private boolean isChatHidden() { throw new AssertionError(); }
    @Shadow private boolean isChatFocused() { throw new AssertionError(); }
    @Shadow private static double getTimeFactor(int age) { throw new AssertionError(); }
    @Shadow private int getLineHeight() { throw new AssertionError(); }
    @Shadow private double screenToChatX(double x) { throw new AssertionError(); }
    @Shadow private double screenToChatY(double y) { throw new AssertionError(); }
    @Shadow private int getMessageEndIndexAt(double x, double y) { throw new AssertionError(); }
    @Shadow private int getTagIconLeft(GuiMessage.Line line) { throw new AssertionError(); }
    @Shadow private void drawTagIcon(GuiGraphics graphics, int x, int y,
            GuiMessageTag.Icon icon) { throw new AssertionError(); }

    @Shadow public abstract int getWidth();
    @Shadow public abstract double getScale();
    @Shadow public abstract int getLinesPerPage();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$renderFacilityChat(GuiGraphics graphics,
            int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        ci.cancel();
        if (this.isChatHidden()) return;

        boolean focused = this.isChatFocused();
        int total = this.trimmedMessages.size();
        if (total <= 0 && !focused) return;

        ChatComponent self = (ChatComponent) (Object) this;
        int linesPerPage = this.getLinesPerPage();
        float scale = (float) Math.max(0.01D, this.getScale());
        int width = Mth.ceil((float) this.getWidth() / scale);
        int lineHeight = this.getLineHeight();
        int top = FacilityChatLayout.topScaled(self);
        int messageTop = FacilityChatLayout.messageTopScaled(self, focused);
        int messageBottom = messageTop + linesPerPage * lineHeight;
        double textOpacity = this.minecraft.options.chatOpacity().get() * 0.9D + 0.1D;
        double backgroundOpacity = this.minecraft.options.textBackgroundOpacity().get();
        double lineSpacing = this.minecraft.options.chatLineSpacing().get();
        int textOffset = (int) Math.round(-8.0D * (lineSpacing + 1.0D)
                + 4.0D * lineSpacing);
        long queued = this.minecraft.getChatListener().queueSize();
        float partialTick = this.minecraft.getFrameTime();
        int openOffsetScreen = focused
                ? FacilityChatLayout.openOffsetScreen(self) : 0;

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(4.0F,
                (float) openOffsetScreen / scale, 0.0F);

        if (focused) {
            scpAdditions$drawFocusedFrame(graphics, width, top,
                    messageTop, messageBottom, backgroundOpacity, queued);
        }

        int available = Math.max(0, total - this.chatScrollbarPos);
        int windowCount = Math.min(linesPerPage, available);
        List<Integer> displayIndices = new ArrayList<>(windowCount);
        for (int offset = 0; offset < windowCount; ++offset) {
            int index = this.chatScrollbarPos + offset;
            GuiMessage.Line line = this.trimmedMessages.get(index);
            int age = tickCount - line.addedTime();
            if (focused || age < 200) displayIndices.add(index);
        }
        Collections.reverse(displayIndices);

        int hoveredEnd = -1;
        if (focused && !displayIndices.isEmpty()) {
            hoveredEnd = this.getMessageEndIndexAt(
                    this.screenToChatX(mouseX), this.screenToChatY(mouseY));
        }

        int row = 0;
        for (int index : displayIndices) {
            GuiMessage.Line line = this.trimmedMessages.get(index);
            int age = tickCount - line.addedTime();
            double fade = focused ? 1.0D : getTimeFactor(age);
            int textAlpha = (int) (255.0D * fade * textOpacity);
            int backgroundAlpha = (int) (255.0D * fade * backgroundOpacity);

            float enter = scpAdditions$messageEnterProgress(age, partialTick);
            int lineSlide = Math.round(-SCP_ADDITIONS_MESSAGE_SLIDE
                    * (1.0F - enter));
            if (age < SCP_ADDITIONS_MESSAGE_ENTER_TICKS) {
                float alphaEnter = 0.35F + 0.65F * enter;
                textAlpha = Math.round(textAlpha * alphaEnter);
                backgroundAlpha = Math.round(backgroundAlpha * alphaEnter);
            }

            int currentRow = row++;
            if (textAlpha <= 3) continue;

            int rowTop = messageTop + currentRow * lineHeight + lineSlide;
            int rowBottom = rowTop + lineHeight;
            int textY = rowBottom + textOffset;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 50.0F);

            if (!focused) {
                graphics.fill(-4, rowTop, width + 8, rowBottom,
                        FacilityChatLayout.withAlpha(FacilityChatLayout.NAVY,
                                Math.min(220, backgroundAlpha)));
                graphics.fill(-4, rowTop, -3, rowBottom,
                        FacilityChatLayout.withAlpha(FacilityChatLayout.BORDER,
                                Math.max(28, textAlpha / 3)));
            }

            GuiMessageTag tag = line.tag();
            if (tag != null) {
                int tagColor = tag.indicatorColor() | (textAlpha << 24);
                graphics.fill(-4, rowTop, -2, rowBottom, tagColor);
                if (index == hoveredEnd && tag.icon() != null) {
                    int iconX = this.getTagIconLeft(line);
                    this.drawTagIcon(graphics, iconX, textY + 9, tag.icon());
                }
            }

            graphics.pose().translate(0.0F, 0.0F, 50.0F);
            graphics.drawString(this.minecraft.font,
                    scpAdditions$roboto(line.content()), 0, textY,
                    0x00FFFFFF | (textAlpha << 24),
                    !ClientModulePreferences.disableTextDropShadows());
            graphics.pose().popPose();
        }

        if (focused) {
            scpAdditions$drawScrollbar(graphics, width, messageTop,
                    linesPerPage * lineHeight, total, linesPerPage);
        }

        graphics.pose().popPose();
    }

    @Unique
    private static float scpAdditions$messageEnterProgress(int age,
            float partialTick) {
        float raw = Mth.clamp((age + partialTick)
                / SCP_ADDITIONS_MESSAGE_ENTER_TICKS, 0.0F, 1.0F);
        float inverse = 1.0F - raw;
        return 1.0F - inverse * inverse * inverse;
    }

    @Unique
    private static FormattedCharSequence scpAdditions$roboto(
            FormattedCharSequence content) {
        return sink -> content.accept((index, style, codePoint) ->
                sink.accept(index, style.withFont(ScpFonts.ROBOTO), codePoint));
    }

    private void scpAdditions$drawFocusedFrame(GuiGraphics graphics,
            int width, int top, int messageTop, int messageBottom,
            double backgroundOpacity, long queued) {
        int bodyAlpha = 72 + (int) Math.round(148.0D * backgroundOpacity);
        graphics.fill(-4, top, width + 8, messageBottom,
                FacilityChatLayout.withAlpha(FacilityChatLayout.NAVY, bodyAlpha));
        graphics.fill(-4, top, width + 8,
                top + FacilityChatLayout.HEADER_HEIGHT,
                FacilityChatLayout.withAlpha(FacilityChatLayout.HEADER, 238));
        graphics.fill(-4, top + FacilityChatLayout.HEADER_HEIGHT - 1,
                width + 8, top + FacilityChatLayout.HEADER_HEIGHT,
                FacilityChatLayout.ACCENT);
        graphics.fill(-4, top, -3, messageBottom, FacilityChatLayout.BORDER);
        graphics.fill(width + 7, top, width + 8, messageBottom,
                FacilityChatLayout.BORDER);
        graphics.fill(-4, messageBottom - 1, width + 8, messageBottom,
                FacilityChatLayout.BORDER);

        Component title = ScpFonts.roboto("//FACILITY_COMMS");
        graphics.drawString(this.minecraft.font, title, 2, top + 5,
                FacilityChatLayout.PALE_GOLD, false);

        String statusText;
        int statusColor;
        if (queued > 0L) {
            statusText = "QUEUE " + queued;
            statusColor = FacilityChatLayout.ACCENT;
        } else if (this.newMessageSinceScroll) {
            statusText = "NEW";
            statusColor = FacilityChatLayout.PALE_GOLD;
        } else {
            statusText = "LIVE";
            statusColor = FacilityChatLayout.LIVE;
        }
        Component status = ScpFonts.roboto(statusText);
        int statusX = width + 3 - this.minecraft.font.width(status);
        graphics.drawString(this.minecraft.font, status, statusX,
                top + 5, statusColor, false);
    }

    private void scpAdditions$drawScrollbar(GuiGraphics graphics, int width,
            int trackTop, int trackHeight, int total, int pageLines) {
        int visible = Math.min(pageLines, total);
        if (total <= visible || trackHeight <= 0) return;

        int maxScroll = Math.max(1, total - visible);
        double progress = 1.0D - Mth.clamp(
                (double) this.chatScrollbarPos / (double) maxScroll,
                0.0D, 1.0D);
        int thumbHeight = Math.max(8, trackHeight * visible / total);
        int thumbTop = trackTop + (int) Math.round(
                (trackHeight - thumbHeight) * progress);
        int x = width + 5;

        graphics.fill(x, trackTop, x + 1, trackTop + trackHeight,
                FacilityChatLayout.withAlpha(FacilityChatLayout.BORDER, 130));
        graphics.fill(x - 1, thumbTop, x + 2, thumbTop + thumbHeight,
                this.newMessageSinceScroll
                        ? FacilityChatLayout.ACCENT
                        : FacilityChatLayout.BORDER_HOVER);
    }

    @Inject(method = "screenToChatY", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$mapTopDownMouseY(double screenY,
            CallbackInfoReturnable<Double> cir) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;

        ChatComponent self = (ChatComponent) (Object) this;
        double scale = Math.max(0.01D, this.getScale());
        int lineHeight = this.getLineHeight();
        int count = Math.min(this.getLinesPerPage(),
                Math.max(0, this.trimmedMessages.size() - this.chatScrollbarPos));
        double animatedScreenY = screenY
                - (this.isChatFocused()
                ? FacilityChatLayout.openOffsetScreen(self) : 0);
        double localY = animatedScreenY / scale
                - FacilityChatLayout.messageTopScaled(self, this.isChatFocused());
        if (count <= 0 || localY < 0.0D
                || localY >= (double) count * lineHeight) {
            cir.setReturnValue(-1.0D);
            return;
        }

        int row = Mth.floor(localY / lineHeight);
        double fraction = (localY - row * lineHeight) / lineHeight;
        cir.setReturnValue((double) (count - 1 - row) + fraction);
    }

    @Inject(method = "handleChatQueueClicked", at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$handleHeaderQueue(double mouseX, double mouseY,
            CallbackInfoReturnable<Boolean> cir) {
        if (!ClientModulePreferences.facilityChatInterfaceEnabled()) return;
        cir.setReturnValue(false);

        if (!this.isChatFocused() || this.minecraft.options.hideGui
                || this.isChatHidden()) return;
        long queued = this.minecraft.getChatListener().queueSize();
        if (queued <= 0L) return;

        ChatComponent self = (ChatComponent) (Object) this;
        double scale = Math.max(0.01D, this.getScale());
        int animationOffset = FacilityChatLayout.openOffsetScreen(self);
        int top = Mth.floor(FacilityChatLayout.topScaled(self) * scale)
                + animationOffset;
        int bottom = Mth.ceil((FacilityChatLayout.topScaled(self)
                + FacilityChatLayout.HEADER_HEIGHT) * scale)
                + animationOffset;
        int right = FacilityChatLayout.panelScreenRight(self);
        int left = Math.max(0, right - 96);
        if (mouseX >= left && mouseX <= right
                && mouseY >= top && mouseY <= bottom) {
            this.minecraft.getChatListener().acceptNextDelayedMessage();
            cir.setReturnValue(true);
        }
    }
}
