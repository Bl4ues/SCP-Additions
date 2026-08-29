package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.data.Scp914SkinManager;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayerParent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Renders a dead player as a low, inert body with a short fake-ragdoll fall. */
public final class PlayerCorpseRenderer extends
        MobRenderer<PlayerCorpseEntity, PlayerCorpseRenderer.CorpseModel> {
    private static final float FALL_TICKS = 16.0F;
    private static final float FINAL_Y_OFFSET = -1.16F;
    // Empirically this axis changes the settled body's floor clearance after the
    // -90 degree collapse. +0.16 hovered; -0.45 buried it. Keep only the tiny
    // positive clearance needed to prevent z-fighting with the block surface.
    private static final float FINAL_Z_OFFSET = 0.04F;
    private static final float[] FINAL_YAW_OFFSETS = {
            -8.0F, 6.0F, -3.0F, 10.0F, -11.0F, 3.0F
    };
    private static final Map<String, ResourceLocation> CUSTOM_SKIN_TEXTURES =
            new HashMap<>();
    private static final Set<String> FAILED_CUSTOM_SKINS = new HashSet<>();

    private final CorpseModel defaultModel;
    private final CorpseModel slimModel;

    public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new CorpseModel(context.bakeLayer(ModelLayers.PLAYER), false),
                0.0F);
        this.defaultModel = this.model;
        this.slimModel = new CorpseModel(
                context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        addLayer(new CorpseArmorLayer(this,
                new HumanoidModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager(), false));
        addLayer(new CorpseArmorLayer(this,
                new HumanoidModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                context.getModelManager(), true));
    }

    @Override
    public void render(PlayerCorpseEntity entity, float entityYaw,
            float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight) {
        this.model = isSlim(entity) ? slimModel : defaultModel;
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    protected void setupRotations(PlayerCorpseEntity entity,
            PoseStack poseStack, float ageInTicks, float rotationYaw,
            float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw,
                partialTick);

        float raw = entity.settled() ? 1.0F : Mth.clamp(
                (entity.tickCount + partialTick) / FALL_TICKS, 0.0F, 1.0F);
        float collapse = raw * raw * (3.0F - 2.0F * raw);
        int variant = entity.poseVariant();
        float side = (variant & 1) == 0 ? -1.0F : 1.0F;

        float fallLean = side * (float) Math.sin(collapse * Math.PI) * 7.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(
                FINAL_YAW_OFFSETS[variant] * collapse));
        poseStack.mulPose(Axis.ZP.rotationDegrees(fallLean));

        float impact = 0.0F;
        if (!entity.settled() && raw > 0.72F) {
            float impactProgress = Mth.clamp((raw - 0.72F) / 0.28F,
                    0.0F, 1.0F);
            impact = (float) Math.sin(impactProgress * Math.PI) * 3.0F;
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(
                -90.0F * collapse - impact));
        poseStack.translate(0.0F, FINAL_Y_OFFSET * collapse,
                FINAL_Z_OFFSET * collapse);
    }

    @Override
    protected void scale(PlayerCorpseEntity entity, PoseStack poseStack,
            float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCorpseEntity entity) {
        ResourceLocation custom = customSkinTexture(entity.customSkin());
        if (custom != null) return custom;

        UUID owner = entity.ownerId();
        PlayerInfo info = playerInfo(owner);
        if (info != null) return info.getSkinLocation();
        UUID fallback = owner == null ? entity.getUUID() : owner;
        return DefaultPlayerSkin.getDefaultSkin(fallback);
    }

    private static ResourceLocation customSkinTexture(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        ResourceLocation cached = CUSTOM_SKIN_TEXTURES.get(fileName);
        if (cached != null) return cached;
        if (FAILED_CUSTOM_SKINS.contains(fileName)) return null;

        Path path = Scp914SkinManager.resolveSkin(fileName);
        if (path == null) {
            FAILED_CUSTOM_SKINS.add(fileName);
            return null;
        }

        NativeImage source = null;
        try (InputStream stream = Files.newInputStream(path)) {
            source = NativeImage.read(stream);
            NativeImage prepared = preparePlayerSkin(source);
            if (prepared == null) {
                source.close();
                FAILED_CUSTOM_SKINS.add(fileName);
                ScpClassifiedDirectiveMod.LOGGER.warn(
                        "SCP-914 corpse skin {} must be 64x64 or legacy 64x32",
                        fileName);
                return null;
            }
            if (prepared != source) source.close();

            DynamicTexture texture = new DynamicTexture(prepared);
            ResourceLocation location = Minecraft.getInstance()
                    .getTextureManager().register(
                            "scp_classified_directive_corpse_skin", texture);
            CUSTOM_SKIN_TEXTURES.put(fileName, location);
            return location;
        } catch (Exception exception) {
            if (source != null) {
                try {
                    source.close();
                } catch (Exception ignored) {
                }
            }
            FAILED_CUSTOM_SKINS.add(fileName);
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Failed to load SCP-914 corpse skin {} from {}",
                    fileName, path, exception);
            return null;
        }
    }

    private static NativeImage preparePlayerSkin(NativeImage source) {
        if (source == null || source.getWidth() != 64) return null;
        if (source.getHeight() == 64) return source;
        if (source.getHeight() != 32) return null;

        NativeImage converted = new NativeImage(64, 64, true);
        converted.copyRect(source, 0, 0, 0, 0,
                64, 32, false, false);

        // Vanilla's legacy-skin expansion. Old 64x32 skins contain only the
        // right arm/leg; mirror those regions into the modern left-side slots.
        converted.copyRect(4, 16, 16, 32, 4, 4, true, false);
        converted.copyRect(8, 16, 16, 32, 4, 4, true, false);
        converted.copyRect(0, 20, 24, 32, 4, 12, true, false);
        converted.copyRect(4, 20, 16, 32, 4, 12, true, false);
        converted.copyRect(8, 20, 8, 32, 4, 12, true, false);
        converted.copyRect(12, 20, 16, 32, 4, 12, true, false);
        converted.copyRect(44, 16, -8, 32, 4, 4, true, false);
        converted.copyRect(48, 16, -8, 32, 4, 4, true, false);
        converted.copyRect(40, 20, 0, 32, 4, 12, true, false);
        converted.copyRect(44, 20, -8, 32, 4, 12, true, false);
        converted.copyRect(48, 20, -16, 32, 4, 12, true, false);
        converted.copyRect(52, 20, -8, 32, 4, 12, true, false);
        return converted;
    }

    private static boolean isSlim(PlayerCorpseEntity entity) {
        if (entity.slimModel()) return true;
        UUID owner = entity.ownerId();
        PlayerInfo info = playerInfo(owner);
        if (info != null) return "slim".equals(info.getModelName());
        UUID fallback = owner == null ? entity.getUUID() : owner;
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(fallback));
    }

    private static PlayerInfo playerInfo(UUID owner) {
        if (owner == null || Minecraft.getInstance().getConnection() == null) {
            return null;
        }
        return Minecraft.getInstance().getConnection().getPlayerInfo(owner);
    }

    private static final class CorpseArmorLayer extends HumanoidArmorLayer<
            PlayerCorpseEntity, CorpseModel, HumanoidModel<PlayerCorpseEntity>> {
        private final boolean slim;

        private CorpseArmorLayer(
                RenderLayerParent<PlayerCorpseEntity, CorpseModel> parent,
                HumanoidModel<PlayerCorpseEntity> innerModel,
                HumanoidModel<PlayerCorpseEntity> outerModel,
                ModelManager modelManager, boolean slim) {
            super(parent, innerModel, outerModel, modelManager);
            this.slim = slim;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, PlayerCorpseEntity entity, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks,
                float netHeadYaw, float headPitch) {
            if (isSlim(entity) != slim) return;
            super.render(poseStack, buffers, packedLight, entity, limbSwing,
                    limbSwingAmount, partialTick, ageInTicks, netHeadYaw,
                    headPitch);
        }
    }

    static final class CorpseModel extends PlayerModel<PlayerCorpseEntity> {
        private static final float[] HEAD_TURNS = {
                -0.26F, 0.30F, -0.18F, 0.22F, -0.34F, 0.12F
        };
        private static final float[] RIGHT_ARM_SPREAD = {
                0.10F, 0.38F, 0.07F, 0.24F, 0.05F, 0.18F
        };
        private static final float[] LEFT_ARM_SPREAD = {
                -0.14F, -0.08F, -0.34F, -0.28F, -0.06F, -0.20F
        };
        private static final float[] RIGHT_LEG_SPREAD = {
                0.035F, 0.075F, 0.045F, 0.025F, 0.090F, 0.055F
        };
        private static final float[] LEFT_LEG_SPREAD = {
                -0.050F, -0.030F, -0.085F, -0.055F, -0.025F, -0.070F
        };

        private CorpseModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(PlayerCorpseEntity entity, float limbSwing,
                float limbSwingAmount, float ageInTicks, float netHeadYaw,
                float headPitch) {
            setAllVisible(true);
            crouching = false;
            swimAmount = 0.0F;
            super.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

            int variant = entity.poseVariant();
            head.xRot = 0.0F;
            head.yRot = HEAD_TURNS[variant];
            head.zRot = 0.0F;
            body.xRot = 0.0F;
            body.yRot = 0.0F;
            body.zRot = 0.0F;

            rightArm.xRot = 0.0F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = RIGHT_ARM_SPREAD[variant];
            leftArm.xRot = 0.0F;
            leftArm.yRot = 0.0F;
            leftArm.zRot = LEFT_ARM_SPREAD[variant];

            rightLeg.xRot = 0.0F;
            rightLeg.yRot = 0.0F;
            rightLeg.zRot = RIGHT_LEG_SPREAD[variant];
            leftLeg.xRot = 0.0F;
            leftLeg.yRot = 0.0F;
            leftLeg.zRot = LEFT_LEG_SPREAD[variant];

            hat.copyFrom(head);
            jacket.copyFrom(body);
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            rightPants.copyFrom(rightLeg);
            leftPants.copyFrom(leftLeg);
        }
    }
}
