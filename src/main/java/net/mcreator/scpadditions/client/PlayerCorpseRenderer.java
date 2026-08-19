package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.PlayerCorpseEntity;

import java.util.UUID;

/** Renders a dead player as a low, inert body with a short fake-ragdoll fall. */
public final class PlayerCorpseRenderer extends
        MobRenderer<PlayerCorpseEntity, PlayerCorpseRenderer.CorpseModel> {
    private static final float FALL_TICKS = 16.0F;
    private static final float FINAL_Y_OFFSET = -1.16F;
    private static final float FINAL_Z_OFFSET = 0.30F;
    private static final float[] FINAL_YAW_OFFSETS = {
            -8.0F, 6.0F, -3.0F, 10.0F, -11.0F, 3.0F
    };

    private final CorpseModel defaultModel;
    private final CorpseModel slimModel;

    public PlayerCorpseRenderer(EntityRendererProvider.Context context) {
        super(context, new CorpseModel(context.bakeLayer(ModelLayers.PLAYER), false),
                0.0F);
        this.defaultModel = this.model;
        this.slimModel = new CorpseModel(
                context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
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

        // A small lateral lean during the fall keeps the transition from looking
        // like a rigid board rotating around its ankles. It returns to zero at
        // rest so every final pose remains properly flat against the floor.
        float fallLean = side * (float) Math.sin(collapse * Math.PI) * 7.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(
                FINAL_YAW_OFFSETS[variant] * collapse));
        poseStack.mulPose(Axis.ZP.rotationDegrees(fallLean));

        // Briefly overshoot the floor angle near impact, then settle back to a
        // clean -90 degree prone pose. This is deliberately modest: fake ragdoll,
        // not a human-shaped spring toy.
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
        UUID owner = entity.ownerId();
        PlayerInfo info = playerInfo(owner);
        if (info != null) return info.getSkinLocation();
        UUID fallback = owner == null ? entity.getUUID() : owner;
        return DefaultPlayerSkin.getDefaultSkin(fallback);
    }

    private static boolean isSlim(PlayerCorpseEntity entity) {
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

            // Keep every limb essentially straight and rotate only in the plane
            // of the floor. The previous frozen swim frame bent the legs upward,
            // which looked less like a body and more like a discarded action figure.
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
