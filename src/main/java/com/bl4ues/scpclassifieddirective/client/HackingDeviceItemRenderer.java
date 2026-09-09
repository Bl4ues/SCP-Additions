package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib renderer for the Hacking Device. Both the held cooldown and the
 * attached hacking session are drawn from this renderer's live model pose so
 * the CRT characters cannot drift away from the actual screen plane.
 */
public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    private static final double SCREEN_TEXT_OFFSET = 0.04D / 16.0D;
    private static final float SCREEN_SCALE = (float)
            (HackingDeviceAttachmentGeometry.SCREEN_WIDTH
                    / HackingDeviceScreenTextClient.LOGICAL_WIDTH);
    private static final ThreadLocal<BlockPos> ATTACHED_RENDER_POS =
            new ThreadLocal<>();

    private ItemStack renderedStack = ItemStack.EMPTY;
    private ItemDisplayContext renderedContext = ItemDisplayContext.NONE;

    public HackingDeviceItemRenderer() {
        super(new HackingDeviceGeoModel());
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    HackingDeviceItem animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                renderPhysicalScreen(poseStack, bufferSource);
            }
        });
    }

    /** Marks ItemDisplayContext.NONE rendering as an attached world device. */
    public static void beginAttachedRender(BlockPos pos) {
        if (pos == null) ATTACHED_RENDER_POS.remove();
        else ATTACHED_RENDER_POS.set(pos.immutable());
    }

    public static void endAttachedRender() {
        ATTACHED_RENDER_POS.remove();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        renderedStack = stack;
        renderedContext = displayContext;
        try {
            super.renderByItem(stack, displayContext, poseStack, bufferSource,
                    packedLight, packedOverlay);
        } finally {
            renderedStack = ItemStack.EMPTY;
            renderedContext = ItemDisplayContext.NONE;
        }
    }

    private void renderPhysicalScreen(PoseStack poseStack,
            MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        BlockPos attachedPos = ATTACHED_RENDER_POS.get();
        boolean attached = attachedPos != null;
        boolean heldCooldown = !attached
                && renderedContext != ItemDisplayContext.NONE
                && !renderedStack.isEmpty()
                && HackingDeviceItem.isCoolingDown(renderedStack,
                        minecraft.level);
        if (!attached && !heldCooldown) return;
        if (!(bufferSource instanceof MultiBufferSource.BufferSource buffers)) {
            return;
        }

        Frame frame = HackingDeviceAttachmentGeometry.localScreenFrame();
        Vec3 right = frame.right().normalize();
        Vec3 down = frame.up().scale(-1.0D).normalize();
        Vec3 normal = frame.outward().normalize();
        Vec3 center = frame.center().add(normal.scale(SCREEN_TEXT_OFFSET));

        Matrix4f physicalScreen = new Matrix4f().identity();
        // Matrix columns are the local logical-screen X/Y/Z basis.
        physicalScreen.m00((float) right.x);
        physicalScreen.m01((float) right.y);
        physicalScreen.m02((float) right.z);
        physicalScreen.m10((float) down.x);
        physicalScreen.m11((float) down.y);
        physicalScreen.m12((float) down.z);
        physicalScreen.m20((float) normal.x);
        physicalScreen.m21((float) normal.y);
        physicalScreen.m22((float) normal.z);
        physicalScreen.m30((float) center.x);
        physicalScreen.m31((float) center.y);
        physicalScreen.m32((float) center.z);

        poseStack.pushPose();
        poseStack.mulPoseMatrix(physicalScreen);
        poseStack.scale(SCREEN_SCALE, SCREEN_SCALE, SCREEN_SCALE);
        poseStack.translate(-HackingDeviceScreenTextClient.LOGICAL_WIDTH * 0.5F,
                -HackingDeviceScreenTextClient.LOGICAL_HEIGHT * 0.5F, 0.0F);
        if (attached) {
            HackingDeviceAttachedRenderer.renderAttachedScreenText(attachedPos,
                    minecraft.font, poseStack, buffers);
        } else {
            HackingDeviceScreenTextClient.renderItemCooldown(renderedStack,
                    minecraft.font, poseStack, buffers);
        }
        poseStack.popPose();
    }
}
