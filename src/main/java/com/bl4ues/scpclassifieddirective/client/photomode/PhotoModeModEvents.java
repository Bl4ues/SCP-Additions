package com.bl4ues.scpclassifieddirective.client.photomode;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class PhotoModeModEvents {
    private PhotoModeModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        if (PhotoModeFeature.isEnabled()) {
            event.register(PhotoModeFeature.OPEN_PHOTO_MODE);
        }
    }
}
