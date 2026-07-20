package net.mcreator.scpadditions.fabric.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$renderPlayerPre(
            AbstractClientPlayer player,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        RenderPlayerEvent.Pre event = new RenderPlayerEvent.Pre(
                player, renderer, partialTick, poseStack, buffers, packedLight);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(
                    player, renderer, partialTick, poseStack, buffers, packedLight));
            callback.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void scpAdditions$renderPlayerPost(
            AbstractClientPlayer player,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(
                player,
                (PlayerRenderer) (Object) this,
                partialTick,
                poseStack,
                buffers,
                packedLight));
    }
}
