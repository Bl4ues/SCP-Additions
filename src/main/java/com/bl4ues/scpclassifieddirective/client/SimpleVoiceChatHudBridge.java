package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;
import com.bl4ues.scpclassifieddirective.vitals.VitalsModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Keeps Simple Voice Chat's Forge HUD lifecycle intact when SCP Inventory
 * replaces the vanilla hotbar, and optionally anchors the main voice icon next
 * to SCP: Classified Directive' health/stamina HUD.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class SimpleVoiceChatHudBridge {
    private static final int VITALS_BAR_X = 52;
    private static final int VITALS_BAR_WIDTH = 184;
    private static final int VITALS_BAR_HEIGHT = 10;
    private static final int VITALS_BOTTOM_MARGIN = 70;
    private static final int VITALS_ROW_GAP = 18;
    private static final int VOICE_ICON_SIZE = 16;
    private static final int VOICE_ICON_GAP = 8;

    private SimpleVoiceChatHudBridge() {
    }

    /**
     * Simple Voice Chat renders its HUD from the vanilla hotbar POST event.
     * SCP Inventory cancels that overlay while its custom hotbar is active, so
     * Forge never emits the corresponding POST event. Re-emit only that missing
     * lifecycle event after the replacement hotbar has rendered.
     */
    public static void restoreCanceledVanillaHotbarPost(GuiGraphics graphics,
            float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!SimpleVoiceChatPresence.installed()
                || !CustomHotbarOverlay.isActiveFor(player)) {
            return;
        }

        MinecraftForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(
                minecraft.getWindow(), graphics, partialTick,
                VanillaGuiOverlay.HOTBAR.type()));
    }

    /** True only while the SCP vitals HUD is actually visible and integration is enabled. */
    public static boolean shouldRelocateHudIcon() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return SimpleVoiceChatPresence.installed()
                && SimpleVoiceChatCompatibilityClientState.known()
                && SimpleVoiceChatCompatibilityClientState.installed()
                && SimpleVoiceChatCompatibilityClientState.enabled()
                && player != null
                && !player.isCreative()
                && !player.isSpectator()
                && minecraft.screen == null
                && !minecraft.options.hideGui
                && VitalsModule.anyHudEnabled();
    }

    /** Draws the same 16px Simple Voice Chat icon at the responsive SCP vitals anchor. */
    public static void renderRelocatedHudIcon(GuiGraphics graphics,
            ResourceLocation texture) {
        int screenHeight = ResponsiveUiScale.current().virtualHeight();
        int x = VITALS_BAR_X + VITALS_BAR_WIDTH + VOICE_ICON_GAP;
        int y = voiceIconY(screenHeight);
        float scale = voiceChatHudScale();

        graphics.pose().pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                VOICE_ICON_SIZE, VOICE_ICON_SIZE,
                VOICE_ICON_SIZE, VOICE_ICON_SIZE);
        graphics.pose().popPose();
    }

    private static int voiceIconY(int screenHeight) {
        int firstRowY = screenHeight - VITALS_BOTTOM_MARGIN;
        int rows = (VitalsModule.staminaHudEnabled() ? 1 : 0)
                + (VitalsModule.healthHudEnabled() ? 1 : 0);
        int bottom = firstRowY + VITALS_BAR_HEIGHT;
        if (rows > 1) {
            bottom = firstRowY + VITALS_ROW_GAP + VITALS_BAR_HEIGHT;
        }
        return firstRowY + (bottom - firstRowY - VOICE_ICON_SIZE) / 2;
    }

    /**
     * Preserve Simple Voice Chat's own HUD scale without linking SCP: Classified Directive
     * against its private client implementation. The integration remains truly
     * optional when the voice-chat mod is absent.
     */
    private static float voiceChatHudScale() {
        try {
            Class<?> clientClass = Class.forName(
                    "de.maxhenkel.voicechat.VoicechatClient", false,
                    SimpleVoiceChatHudBridge.class.getClassLoader());
            Field configField = clientClass.getField("CLIENT_CONFIG");
            Object config = configField.get(null);
            if (config == null) return 1.0F;

            Field scaleField = config.getClass().getField("hudIconScale");
            Object scaleEntry = scaleField.get(config);
            if (scaleEntry == null) return 1.0F;

            Method get = scaleEntry.getClass().getMethod("get");
            Object value = get.invoke(scaleEntry);
            if (value instanceof Number number) {
                return Math.max(0.01F,
                        Math.min(10.0F, number.floatValue()));
            }
        } catch (ReflectiveOperationException | LinkageError
                | RuntimeException ignored) {
            // SVC changed an internal client detail. Default scale still keeps
            // the icon usable rather than making an optional integration fatal.
        }
        return 1.0F;
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        SimpleVoiceChatCompatibilityClientState.reset();
        if (!SimpleVoiceChatPresence.installed()) return;
        Minecraft.getInstance().execute(
                SimpleVoiceChatCompatibilityClientState::query);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SimpleVoiceChatCompatibilityClientState.reset();
    }
}
