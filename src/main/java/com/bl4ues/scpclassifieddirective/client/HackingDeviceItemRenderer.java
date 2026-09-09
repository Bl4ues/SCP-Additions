package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib renderer for the Hacking Device.
 *
 * The authored `screen` bone is the source of truth for both screen cases. For a
 * reader-attached device we capture the exact logical-screen matrix while Gecko
 * walks that bone and let the world renderer paint characters only after the
 * model batch has been flushed. For a held cooldown we can paint immediately,
 * but still split the model and font passes explicitly.
 */
public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    /** Slightly in front of the authored flat screen to avoid z-fighting. */
    private static final double SCREEN_TEXT_OFFSET = 0.0078D;
    private static final ThreadLocal<BlockPos> ATTACHED_TARGET =
            new ThreadLocal<>();
    private static final ThreadLocal<Matrix4f> ATTACHED_SCREEN_TRANSFORM =
            new ThreadLocal<>();

    private ItemStack renderedStack = ItemStack.EMPTY;
    private ItemDisplayContext renderedContext = ItemDisplayContext.NONE;

    public HackingDeviceItemRenderer() {
        super(new HackingDeviceGeoModel());
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void renderForBone(PoseStack poseStack,
                    HackingDeviceItem animatable, GeoBone bone,
                    RenderType renderType, MultiBufferSource bufferSource,
                    VertexConsumer buffer, float partialTick, int packedLight,
                    int packedOverlay) {
                if ("screen".equals(bone.getName())) {
                    captureOrRenderPhysicalScreen(poseStack, bone, renderType,
                            bufferSource);
                }
            }
        });
    }

    /** Marks an ItemDisplayContext.NONE render as the device attached to a reader. */
    public static void beginAttachedRender(BlockPos pos) {
        ATTACHED_SCREEN_TRANSFORM.remove();
        if (pos != null) ATTACHED_TARGET.set(pos.immutable());
    }

    /**
     * Returns the exact matrix of the authored screen plane captured during the
     * most recent attached render on this thread. The caller owns the returned
     * copy and may safely use it after GeckoLib has finished the body pass.
     */
    public static Matrix4f takeAttachedScreenTransform() {
        Matrix4f transform = ATTACHED_SCREEN_TRANSFORM.get();
        ATTACHED_SCREEN_TRANSFORM.remove();
        return transform == null ? null : new Matrix4f(transform);
    }

    public static void endAttachedRender() {
        ATTACHED_TARGET.remove();
        ATTACHED_SCREEN_TRANSFORM.remove();
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

    private void captureOrRenderPhysicalScreen(PoseStack poseStack, GeoBone bone,
            RenderType originalRenderType, MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !(bufferSource instanceof MultiBufferSource.BufferSource buffers)
                || bone.getCubes().isEmpty()) {
            return;
        }

        BlockPos attachedPos = ATTACHED_TARGET.get();
        boolean attached = attachedPos != null;
        boolean heldCooldown = !attached
                && renderedContext != ItemDisplayContext.NONE
                && !renderedStack.isEmpty()
                && HackingDeviceItem.isCoolingDown(renderedStack,
                        minecraft.level);
        if (!attached && !heldCooldown) return;

        /*
         * renderForBone is invoked after GeckoLib has applied the `screen` bone
         * transform and submitted that bone's cube. Apply only the cube's own
         * pivot/rotation, then use the NORTH quad itself as the logical screen.
         */
        GeoCube screenCube = bone.getCubes().get(0);
        GeoQuad screenQuad = null;
        for (GeoQuad quad : screenCube.quads()) {
            if (quad != null && quad.direction() == Direction.NORTH) {
                screenQuad = quad;
                break;
            }
        }
        if (screenQuad == null || screenQuad.vertices().length < 4) return;

        GeoVertex[] vertices = screenQuad.vertices();
        Vector3f topLeft = vertices[0].position();
        Vector3f topRight = vertices[1].position();
        Vector3f bottomLeft = vertices[3].position();
        Vector3f northNormal = screenQuad.normal();

        float width = distance(topLeft, topRight);
        float height = distance(topLeft, bottomLeft);
        if (width <= 0.00001F || height <= 0.00001F) return;

        float pixelScaleX = width / HackingDeviceScreenTextClient.LOGICAL_WIDTH;
        float pixelScaleY = height / HackingDeviceScreenTextClient.LOGICAL_HEIGHT;
        float depthScale = Math.min(pixelScaleX, pixelScaleY);

        poseStack.pushPose();
        applyCubeTransform(poseStack, screenCube);

        /*
         * The approved operation camera sees the opposite side of the authored
         * NORTH face. Offsetting text along NORTH therefore buried every glyph
         * behind the opaque zero-thickness CRT plane. Move it toward the actual
         * visible side instead. The same fix applies to the five-second timer in
         * hand because both are drawn from this exact screen cube.
         */
        poseStack.translate(
                topLeft.x() - northNormal.x() * SCREEN_TEXT_OFFSET,
                topLeft.y() - northNormal.y() * SCREEN_TEXT_OFFSET,
                topLeft.z() - northNormal.z() * SCREEN_TEXT_OFFSET);
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);

        if (attached) {
            ATTACHED_SCREEN_TRANSFORM.set(
                    new Matrix4f(poseStack.last().pose()));
        } else {
            buffers.endBatch();
            HackingDeviceScreenTextClient.renderItemCooldown(renderedStack,
                    minecraft.font, poseStack, buffers);
            buffers.endBatch();
            bufferSource.getBuffer(originalRenderType);
        }
        poseStack.popPose();
    }

    /** Mirrors GeckoLib's cube-pivot transform without depending on internal util APIs. */
    private static void applyCubeTransform(PoseStack poseStack, GeoCube cube) {
        Vec3 pivot = cube.pivot();
        Vec3 rotation = cube.rotation();
        poseStack.translate(pivot.x / 16.0D, pivot.y / 16.0D,
                pivot.z / 16.0D);
        if (rotation.z != 0.0D) {
            poseStack.mulPose(Axis.ZP.rotation((float) rotation.z));
        }
        if (rotation.y != 0.0D) {
            poseStack.mulPose(Axis.YP.rotation((float) rotation.y));
        }
        if (rotation.x != 0.0D) {
            poseStack.mulPose(Axis.XP.rotation((float) rotation.x));
        }
        poseStack.translate(-pivot.x / 16.0D, -pivot.y / 16.0D,
                -pivot.z / 16.0D);
    }

    private static float distance(Vector3f first, Vector3f second) {
        float x = second.x() - first.x();
        float y = second.y() - first.y();
        float z = second.z() - first.z();
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
}
