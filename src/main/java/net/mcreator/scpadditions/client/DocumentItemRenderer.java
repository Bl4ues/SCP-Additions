package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.document.DocumentData;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * Renders the authored thin 3D Document model while allowing the north/front
 * face to use the image that visually represents a configured document.
 */
public final class DocumentItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/item/document.png");
    private static final ResourceLocation TEMPLATE_TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/gui/document_template.png");

    // Authored Blockbench cuboid, normalized from 0..16 model coordinates.
    private static final float X0 = 5.75F / 16.0F;
    private static final float X1 = 10.25F / 16.0F;
    private static final float Y0 = 3.25F / 16.0F;
    private static final float Y1 = 10.75F / 16.0F;
    private static final float Z0 = 8.25F / 16.0F;
    private static final float Z1 = 8.30F / 16.0F;

    public DocumentItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        ResourceLocation frontTexture = resolveFrontTexture(stack);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer base = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(BASE_TEXTURE));

        // Every non-front face always keeps the authored Blank Document skin.
        east(base, pose, packedLight, packedOverlay,
                0.0F, 8.0F, 0.5F, 15.5F);
        south(base, pose, packedLight, packedOverlay,
                5.0F, 0.0F, 9.5F, 7.5F);
        west(base, pose, packedLight, packedOverlay,
                1.0F, 8.0F, 1.5F, 15.5F);
        up(base, pose, packedLight, packedOverlay,
                6.5F, 8.5F, 2.0F, 8.0F);
        down(base, pose, packedLight, packedOverlay,
                11.5F, 8.0F, 7.0F, 8.5F);

        if (frontTexture == null) {
            north(base, pose, packedLight, packedOverlay,
                    0.0F, 0.0F, 4.5F, 7.5F, false);
            return;
        }

        // Configured/template artwork replaces only the north face. Mapping
        // the full source image here avoids bleeding the artwork onto the
        // document edges, back, or thickness.
        VertexConsumer front = bufferSource.getBuffer(
                RenderType.entityTranslucent(frontTexture));
        north(front, pose, packedLight, packedOverlay,
                0.0F, 0.0F, 16.0F, 16.0F, true);
    }

    private static ResourceLocation resolveFrontTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        if (DocumentData.hasStructuredData(stack)) {
            DocumentData.State state = DocumentData.read(stack);
            if (hasTemplateAppearance(state)) {
                return TEMPLATE_TEXTURE;
            }
            // A genuinely blank dedicated document deliberately retains the
            // authored document.png front rather than the Codex page template.
            return null;
        }

        Optional<CodexDocumentDefinition> configured =
                ScpItemClassifier.getCodexDocument(stack);
        if (configured.isEmpty()) return null;

        CodexDocumentDefinition definition = configured.get();
        if (!definition.getWorldImageKey().isBlank()) {
            return CodexAssetClient.getTexture(definition.getWorldImageKey())
                    .orElse(null);
        }
        return definition.getImageLocation().orElse(null);
    }

    private static boolean hasTemplateAppearance(DocumentData.State state) {
        if (state == null) return false;
        if (state.template() != DocumentData.Template.BLANK_DOCUMENT) return true;
        return !blank(state.header1()) || !blank(state.value1())
                || !blank(state.header2()) || !blank(state.value2())
                || !blank(state.header3()) || !blank(state.value3())
                || !blank(state.body()) || !blank(state.photoKey())
                || !blank(state.caption());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static void north(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1,
            boolean fullTexture) {
        float left = fullTexture ? 0.0F : uv(u0);
        float top = fullTexture ? 0.0F : uv(v0);
        float right = fullTexture ? 1.0F : uv(u1);
        float bottom = fullTexture ? 1.0F : uv(v1);
        vertex(out, pose, X0, Y1, Z0, left, top, light, overlay, 0, 0, -1);
        vertex(out, pose, X0, Y0, Z0, left, bottom, light, overlay, 0, 0, -1);
        vertex(out, pose, X1, Y0, Z0, right, bottom, light, overlay, 0, 0, -1);
        vertex(out, pose, X1, Y1, Z0, right, top, light, overlay, 0, 0, -1);
    }

    private static void south(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X1, Y1, Z1, uv(u0), uv(v0), light, overlay, 0, 0, 1);
        vertex(out, pose, X1, Y0, Z1, uv(u0), uv(v1), light, overlay, 0, 0, 1);
        vertex(out, pose, X0, Y0, Z1, uv(u1), uv(v1), light, overlay, 0, 0, 1);
        vertex(out, pose, X0, Y1, Z1, uv(u1), uv(v0), light, overlay, 0, 0, 1);
    }

    private static void east(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X1, Y1, Z0, uv(u0), uv(v0), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y0, Z0, uv(u0), uv(v1), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y0, Z1, uv(u1), uv(v1), light, overlay, 1, 0, 0);
        vertex(out, pose, X1, Y1, Z1, uv(u1), uv(v0), light, overlay, 1, 0, 0);
    }

    private static void west(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y1, Z1, uv(u0), uv(v0), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y0, Z1, uv(u0), uv(v1), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y0, Z0, uv(u1), uv(v1), light, overlay, -1, 0, 0);
        vertex(out, pose, X0, Y1, Z0, uv(u1), uv(v0), light, overlay, -1, 0, 0);
    }

    private static void up(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y1, Z1, uv(u0), uv(v0), light, overlay, 0, 1, 0);
        vertex(out, pose, X0, Y1, Z0, uv(u0), uv(v1), light, overlay, 0, 1, 0);
        vertex(out, pose, X1, Y1, Z0, uv(u1), uv(v1), light, overlay, 0, 1, 0);
        vertex(out, pose, X1, Y1, Z1, uv(u1), uv(v0), light, overlay, 0, 1, 0);
    }

    private static void down(VertexConsumer out, PoseStack.Pose pose,
            int light, int overlay, float u0, float v0, float u1, float v1) {
        vertex(out, pose, X0, Y0, Z0, uv(u0), uv(v0), light, overlay, 0, -1, 0);
        vertex(out, pose, X0, Y0, Z1, uv(u0), uv(v1), light, overlay, 0, -1, 0);
        vertex(out, pose, X1, Y0, Z1, uv(u1), uv(v1), light, overlay, 0, -1, 0);
        vertex(out, pose, X1, Y0, Z0, uv(u1), uv(v0), light, overlay, 0, -1, 0);
    }

    private static float uv(float modelUv) {
        return modelUv / 16.0F;
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose,
            float x, float y, float z, float u, float v,
            int light, int overlay, float nx, float ny, float nz) {
        out.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }
}
