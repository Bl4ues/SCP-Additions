package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;

/**
 * GeckoLib renderer for the Hacking Device.
 *
 * Both the attached-session UI and the hand-held passage timer are painted from
 * the live GeckoLib pose of the authored `screen` bone. This deliberately avoids
 * rebuilding the tiny CRT plane from hand-maintained model coordinates: the
 * renderer uses the actual baked 2.5 x 1.5 zero-thickness cube and inherits the
 * body's 180-degree rotation plus the screen cube's -22.5-degree rotation.
 */
public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    /** Slightly in front of the authored flat screen to avoid z-fighting. */
    private static final double SCREEN_TEXT_OFFSET = 0.0078D;
    private static final ThreadLocal<BlockPos> ATTACHED_TARGET =
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
                    renderPhysicalScreen(poseStack, bone, renderType,
                            bufferSource);
                }
            }
        });
    }

    /** Marks an ItemDisplayContext.NONE render as the device attached to a reader. */
    public static void beginAttachedRender(BlockPos pos) {
        if (pos != null) ATTACHED_TARGET.set(pos.immutable());
    }

    public static void endAttachedRender() {
        ATTACHED_TARGET.remove();
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

    private void renderPhysicalScreen(PoseStack poseStack, GeoBone bone,
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
         * The screen bone contains exactly the authored flat CRT cube. Its NORTH
         * quad is the visible face (raw model z=-1.05). GeckoLib has already
         * applied the parent bone transforms when renderForBone is called; apply
         * only this cube's own pivot/rotation, exactly as GeoRenderer does while
         * drawing the cube itself.
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
        Vector3f normal = screenQuad.normal();

        float width = distance(topLeft, topRight);
        float height = distance(topLeft, bottomLeft);
        if (width <= 0.00001F || height <= 0.00001F) return;

        float pixelScaleX = width / HackingDeviceScreenTextClient.LOGICAL_WIDTH;
        float pixelScaleY = height / HackingDeviceScreenTextClient.LOGICAL_HEIGHT;
        float depthScale = Math.min(pixelScaleX, pixelScaleY);

        poseStack.pushPose();
        RenderUtil.translateToPivotPoint(poseStack, screenCube);
        RenderUtil.rotateMatrixAroundCube(poseStack, screenCube);
        RenderUtil.translateAwayFromPivotPoint(poseStack, screenCube);
        poseStack.translate(
                topLeft.x() + normal.x() * SCREEN_TEXT_OFFSET,
                topLeft.y() + normal.y() * SCREEN_TEXT_OFFSET,
                topLeft.z() + normal.z() * SCREEN_TEXT_OFFSET);
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);

        if (attached) {
            HackingDeviceAttachedRenderer.renderAttachedScreenText(attachedPos,
                    minecraft.font, poseStack, buffers);
        } else {
            HackingDeviceScreenTextClient.renderItemCooldown(renderedStack,
                    minecraft.font, poseStack, buffers);
        }
        poseStack.popPose();

        // Font rendering selects its own buffers. Restore the model's buffer as
        // required by GeoRenderLayer before GeckoLib continues recursion.
        bufferSource.getBuffer(originalRenderType);
    }

    private static float distance(Vector3f first, Vector3f second) {
        float x = second.x() - first.x();
        float y = second.y() - first.y();
        float z = second.z() - first.z();
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
}
