package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib renderer for the handheld Hacking Device and its physical CRT. */
public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    private static final double SCREEN_TEXT_OFFSET = 0.0080D;
    private static final float MIN_FACE_AREA = 0.0000001F;

    private ItemStack renderedStack = ItemStack.EMPTY;
    private ItemDisplayContext renderedContext = ItemDisplayContext.NONE;
    private Matrix4f heldScreenTransform;

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
                    captureHeldPhysicalScreen(poseStack, bone);
                }
            }
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        renderedStack = stack;
        renderedContext = displayContext;
        heldScreenTransform = null;
        try {
            super.renderByItem(stack, displayContext, poseStack, bufferSource,
                    packedLight, packedOverlay);

            if (heldScreenTransform != null
                    && displayContext != ItemDisplayContext.NONE
                    && Minecraft.getInstance().level != null
                    && HackingDeviceItem.isCoolingDown(stack,
                            Minecraft.getInstance().level)) {
                MultiBufferSource.BufferSource direct =
                        bufferSource instanceof MultiBufferSource.BufferSource source
                                ? source : null;
                if (direct != null) direct.endBatch();

                /*
                 * The screen bone still supplies the exact hand/item transform,
                 * but the countdown itself is now emitted as opaque pixel quads.
                 * This avoids the same Font RenderType failure that kept the
                 * attached minigame completely black.
                 */
                HackingDevicePixelFont.beginMatrix(bufferSource,
                        heldScreenTransform);
                try {
                    HackingDeviceScreenTextClient.renderItemCooldown(stack,
                            Minecraft.getInstance().font, poseStack, bufferSource);
                } finally {
                    HackingDevicePixelFont.end();
                }
                HackingDevicePixelFont.flush(bufferSource);
            }
        } finally {
            heldScreenTransform = null;
            renderedStack = ItemStack.EMPTY;
            renderedContext = ItemDisplayContext.NONE;
        }
    }

    private void captureHeldPhysicalScreen(PoseStack poseStack, GeoBone bone) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || bone.getCubes().isEmpty()
                || renderedContext == ItemDisplayContext.NONE
                || renderedStack.isEmpty()
                || !HackingDeviceItem.isCoolingDown(renderedStack,
                        minecraft.level)) {
            return;
        }

        GeoCube screenCube = bone.getCubes().get(0);
        poseStack.pushPose();
        applyCubeTransform(poseStack, screenCube);
        heldScreenTransform = buildLogicalScreenTransform(screenCube,
                poseStack.last().pose());
        poseStack.popPose();
    }

    /** Builds logical CRT coordinates from the real transformed screen quad. */
    private static Matrix4f buildLogicalScreenTransform(GeoCube cube,
            Matrix4f renderedCubePose) {
        ScreenFace bestFacing = null;
        ScreenFace bestAny = null;

        for (GeoQuad quad : cube.quads()) {
            ScreenFace candidate = screenFace(quad, renderedCubePose);
            if (candidate == null) continue;
            if (bestAny == null || candidate.area() > bestAny.area()) {
                bestAny = candidate;
            }
            if (candidate.facesCamera()
                    && (bestFacing == null
                    || candidate.area() > bestFacing.area())) {
                bestFacing = candidate;
            }
        }

        ScreenFace face = bestFacing != null ? bestFacing : bestAny;
        if (face == null) return null;

        Vector3f right = new Vector3f(face.topRight()).sub(face.topLeft());
        Vector3f down = new Vector3f(face.bottomLeft()).sub(face.topLeft());
        float width = right.length();
        float height = down.length();
        if (width <= 0.00001F || height <= 0.00001F) return null;
        right.div(width);
        down.div(height);

        Vector3f visibleNormal = new Vector3f(right).cross(down).normalize();
        Vector3f center = new Vector3f(face.topLeft())
                .add(face.topRight()).add(face.bottomLeft())
                .add(face.bottomRight()).mul(0.25F);
        Vector3f toCamera = new Vector3f(center).negate();
        if (visibleNormal.dot(toCamera) < 0.0F) visibleNormal.negate();

        float pixelScaleX = width
                / HackingDeviceScreenTextClient.LOGICAL_WIDTH;
        float pixelScaleY = height
                / HackingDeviceScreenTextClient.LOGICAL_HEIGHT;
        float depthScale = Math.min(pixelScaleX, pixelScaleY);
        Vector3f origin = new Vector3f(face.topLeft()).fma(
                (float) SCREEN_TEXT_OFFSET, visibleNormal);

        Matrix4f result = new Matrix4f().identity();
        result.m00(right.x() * pixelScaleX);
        result.m01(right.y() * pixelScaleX);
        result.m02(right.z() * pixelScaleX);
        result.m10(down.x() * pixelScaleY);
        result.m11(down.y() * pixelScaleY);
        result.m12(down.z() * pixelScaleY);
        result.m20(visibleNormal.x() * depthScale);
        result.m21(visibleNormal.y() * depthScale);
        result.m22(visibleNormal.z() * depthScale);
        result.m30(origin.x());
        result.m31(origin.y());
        result.m32(origin.z());
        return result;
    }

    private static ScreenFace screenFace(GeoQuad quad, Matrix4f pose) {
        if (quad == null || quad.vertices() == null
                || quad.vertices().length < 4) {
            return null;
        }

        GeoVertex[] vertices = quad.vertices();
        Vector3f p0 = transform(vertices[0].position(), pose);
        Vector3f p1 = transform(vertices[1].position(), pose);
        Vector3f p2 = transform(vertices[2].position(), pose);
        Vector3f p3 = transform(vertices[3].position(), pose);

        Vector3f edgeRight = new Vector3f(p1).sub(p0);
        Vector3f edgeDown = new Vector3f(p3).sub(p0);
        float area = edgeRight.length() * edgeDown.length();
        if (area <= MIN_FACE_AREA) return null;

        Vector3f normal = new Vector3f(edgeRight).cross(edgeDown);
        if (normal.lengthSquared() <= MIN_FACE_AREA) return null;
        normal.normalize();

        Vector3f center = new Vector3f(p0).add(p1).add(p2).add(p3)
                .mul(0.25F);
        boolean facesCamera = normal.dot(new Vector3f(center).negate()) >= 0.0F;
        return new ScreenFace(p0, p1, p3, p2, area, facesCamera);
    }

    private static Vector3f transform(Vector3f position, Matrix4f matrix) {
        Vector4f transformed = matrix.transform(new Vector4f(
                position.x(), position.y(), position.z(), 1.0F));
        return new Vector3f(transformed.x(), transformed.y(), transformed.z());
    }

    /** Mirrors GeckoLib 4.4.9's cube-pivot transform. */
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

    private record ScreenFace(Vector3f topLeft, Vector3f topRight,
            Vector3f bottomLeft, Vector3f bottomRight, float area,
            boolean facesCamera) {
    }
}
