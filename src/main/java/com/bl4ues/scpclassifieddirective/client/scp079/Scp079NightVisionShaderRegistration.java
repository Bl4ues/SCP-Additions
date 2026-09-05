package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Registers the monochrome SCP-079 low-light camera shader. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp079NightVisionShaderRegistration {
    private Scp079NightVisionShaderRegistration() { }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event)
            throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                                "scp079_night_vision"),
                        DefaultVertexFormat.POSITION_TEX),
                Scp079NightVisionPostProcessor::setShader);
    }
}
