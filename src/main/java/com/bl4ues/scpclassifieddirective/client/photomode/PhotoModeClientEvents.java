package com.bl4ues.scpclassifieddirective.client.photomode;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class PhotoModeClientEvents {
    private PhotoModeClientEvents() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!PhotoModeFeature.isEnabled()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        while (PhotoModeFeature.OPEN_PHOTO_MODE.consumeClick()) {
            if (PhotoModeCapture.isActive()) {
                PhotoModeCapture.cancel();
            } else if (minecraft.screen == null) {
                PhotoModeCapture.open();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        PhotoModeCapture.recordWorldFrame(event);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        PhotoModeCapture.beforeGui(event);
    }
}
