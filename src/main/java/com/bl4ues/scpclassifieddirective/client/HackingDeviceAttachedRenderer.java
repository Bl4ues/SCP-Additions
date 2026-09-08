package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Set;

/** Renders the handheld model physically seated on Keycard Readers and OCUs. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceAttachedRenderer {
    private static final float LOGICAL_WIDTH = 256.0F;
    private static final float LOGICAL_HEIGHT = 154.0F;
    private static final float SCREEN_SCALE = (float)
            (HackingDeviceAttachmentGeometry.SCREEN_WIDTH / LOGICAL_WIDTH);
    private static final int GREEN = 0xFF55FF79;
    private static final double SCREEN_EPSILON = 0.0015D;
    private static final ResourceLocation SCREEN_MASK = new ResourceLocation(
            "minecraft", "textures/block/white_concrete.png");

    private HackingDeviceAttachedRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        Set<BlockPos> visibleDevices = HackingDeviceClientState.snapshot();

        for (BlockPos pos : visibleDevices) {
            if (!minecraft.level.hasChunkAt(pos)
                    || camera.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0D) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos, state);
            if (attachment == null) continue;

            renderDevice(minecraft, poseStack, buffers, camera, pos,
                    attachment);
        }
        // Body/PBR passes finish before any world-space screen layer.
        buffers.endBatch();

        for (BlockPos pos : visibleDevices) {
            if (!minecraft.level.hasChunkAt(pos)
                    || camera.distanceToSqr(Vec3.atCenterOf(pos)) > 4096.0D) {
                continue;
            }
            Attachment attachment = HackingDeviceAttachmentGeometry.resolve(pos,
                    minecraft.level.getBlockState(pos));
            if (attachment != null) {
                renderScreen(minecraft, poseStack, buffers, camera, attachment);
            }
        }
        buffers.endBatch();
    }

    private static void renderDevice(Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera, BlockPos pos,
            Attachment attachment) {
        Vec3 origin = attachment.modelOrigin().subtract(camera);
        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())));
        int light = LevelRenderer.getLightColor(minecraft.level, pos);
        ItemStack deviceStack = new ItemStack(
                ScpClassifiedDirectiveModItems.HACKING_DEVICE.get());
        minecraft.getItemRenderer().renderStatic(deviceStack,
                ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, minecraft.level, 0);
        poseStack.popPose();
    }

    private static void renderScreen(Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            Attachment attachment) {
        Frame frame = attachment.screen();
        RenderType panelType = RenderType.entityCutoutNoCull(SCREEN_MASK);
        VertexConsumer panel = buffers.getBuffer(panelType);
        emitBlackPanel(panel, poseStack.last(), frame, camera);
        // Explicitly flush the opaque/cutout panel before emissive text. This
        // keeps shaders from reordering a coplanar-looking text/background pair.
        buffers.endBatch(panelType);

        Vec3 center = frame.center().subtract(camera)
                .add(frame.outward().scale(SCREEN_EPSILON * 2.0D));
        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())
                        + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        poseStack.scale(-SCREEN_SCALE, -SCREEN_SCALE, SCREEN_SCALE);
        poseStack.translate(-LOGICAL_WIDTH * 0.5F,
                -LOGICAL_HEIGHT * 0.5F, 0.0F);

        Font font = minecraft.font;
        String title = "Hacking Device";
        String message = "This device is not implemented yet";
        float titleX = (LOGICAL_WIDTH - font.width(title)) * 0.5F;
        float messageX = (LOGICAL_WIDTH - font.width(message)) * 0.5F;
        font.drawInBatch(title, titleX, 58.0F, GREEN, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        font.drawInBatch(message, messageX, 76.0F, GREEN, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void emitBlackPanel(VertexConsumer consumer,
            PoseStack.Pose pose, Frame frame, Vec3 camera) {
        Vec3 offset = frame.outward().scale(SCREEN_EPSILON);
        Vec3 topLeft = frame.point(-0.5D, 0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 topRight = frame.point(0.5D, 0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 bottomRight = frame.point(0.5D, -0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 bottomLeft = frame.point(-0.5D, -0.5D, 0.0D)
                .add(offset).subtract(camera);
        Vec3 normalVector = frame.outward();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, topLeft, 0.0F, 0.0F, normalVector);
        vertex(consumer, matrix, normal, topRight, 1.0F, 0.0F, normalVector);
        vertex(consumer, matrix, normal, bottomRight, 1.0F, 1.0F, normalVector);
        vertex(consumer, matrix, normal, bottomLeft, 0.0F, 1.0F, normalVector);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, Vec3 point, float u, float v, Vec3 normalVector) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(0, 0, 0, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, (float) normalVector.x,
                        (float) normalVector.y, (float) normalVector.z)
                .endVertex();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HackingDeviceClientState.clear();
        HackingDeviceFocusClient.forceClear();
    }
}
