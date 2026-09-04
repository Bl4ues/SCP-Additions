package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.MainMenuPlayPanelsClient;
import com.bl4ues.scpclassifieddirective.init.MainMenuSounds;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the singleplayer/multiplayer flyouts consistent with the compact Settings flyout. */
@Mixin(value = MainMenuPlayPanelsClient.class, remap = false)
public abstract class MainMenuPlayPanelsPolishMixin {
    @ModifyConstant(method = "layout", constant = @Constant(floatValue = 0.365F))
    private static float scpClassifiedDirective$matchSettingsWidthRatio(float original) {
        return 0.205F;
    }

    @ModifyConstant(method = "layout", constant = @Constant(intValue = 310))
    private static int scpClassifiedDirective$matchSettingsMinWidth(int original) {
        return 168;
    }

    @ModifyConstant(method = "layout", constant = @Constant(intValue = 470))
    private static int scpClassifiedDirective$matchSettingsMaxWidth(int original) {
        return 235;
    }

    @ModifyConstant(method = "layout", constant = @Constant(intValue = 230))
    private static int scpClassifiedDirective$matchSettingsFallbackWidth(int original) {
        return 168;
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(floatValue = 0.88F))
    private static float scpClassifiedDirective$enlargeWorldDate(float original) {
        return 1.0F;
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(floatValue = 0.82F))
    private static float scpClassifiedDirective$enlargeServerMotd(float original) {
        return 0.96F;
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(floatValue = 0.78F))
    private static float scpClassifiedDirective$enlargeServerPlayers(float original) {
        return 0.92F;
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(floatValue = 0.86F))
    private static float scpClassifiedDirective$enlargeActionSubtitle(float original) {
        return 1.0F;
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(stringValue = "Create New World"))
    private static String scpClassifiedDirective$renameCreateWorld(String original) {
        return "New Game";
    }

    @ModifyConstant(method = "drawRow", constant = @Constant(stringValue = "Start a new local world"))
    private static String scpClassifiedDirective$renameCreateWorldSubtitle(String original) {
        return "Start a new world";
    }

    /*
     * The custom multiplayer flyout performs the same status pings as vanilla,
     * but it owns the pinger lifecycle itself. A failed socket must become an
     * unavailable row, never an exception escaping the title-screen click.
     */
    @Redirect(method = "ping", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ServerStatusPinger;pingServer(Lnet/minecraft/client/multiplayer/ServerData;Ljava/lang/Runnable;)V",
            remap = true))
    private static void scpClassifiedDirective$guardServerPing(
            ServerStatusPinger pinger, ServerData server, Runnable callback) {
        try {
            pinger.pingServer(server, callback);
        } catch (Exception exception) {
            scpClassifiedDirective$markServerUnavailable(server);
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Multiplayer status ping failed; keeping the server row available",
                    exception);
        }
    }

    @Redirect(method = "onClientTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ServerStatusPinger;tick()V",
            remap = true))
    private static void scpClassifiedDirective$guardServerPingerTick(
            ServerStatusPinger pinger) {
        try {
            pinger.tick();
        } catch (Exception exception) {
            pinger.removeAll();
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Stopped multiplayer status polling after a connection failure",
                    exception);
        }
    }

    private static void scpClassifiedDirective$markServerUnavailable(ServerData server) {
        server.motd = Component.translatable("multiplayer.status.cannot_connect");
        server.status = CommonComponents.EMPTY;
        server.ping = -1L;
    }

    @Inject(method = "playHover", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$useMenuHoverSound(CallbackInfo callback) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(MainMenuSounds.HOVER.get(), 1.0F));
        callback.cancel();
    }

    @Inject(method = "playSelect", at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$useMenuSelectSound(CallbackInfo callback) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        ScpClassifiedDirectiveModSounds.SELECT.get(), 1.0F, 0.35F));
        callback.cancel();
    }
}
