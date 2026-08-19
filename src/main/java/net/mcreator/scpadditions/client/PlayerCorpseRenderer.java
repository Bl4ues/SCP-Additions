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
import net.mcreator.scpadditions.entity.PlayerCorpseEntity;

import java.util.UUID;

/** Renders a dead player as a frozen vanilla crawl/prone pose. */
public final class PlayerCorpseRenderer extends
        MobRenderer<PlayerCorpseEntity, PlayerCorpseRenderer.CorpseModel> {
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
        // Same world-space body transform vanilla uses for crawling/swimming,
        // but frozen instead of driven by movement animation.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.translate(0.0F, -1.0F, 0.30F);
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
        private CorpseModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(PlayerCorpseEntity entity, float limbSwing,
                float limbSwingAmount, float ageInTicks, float netHeadYaw,
                float headPitch) {
            setAllVisible(true);
            crouching = false;
            swimAmount = 1.0F;
            // A fixed zero-time swim frame supplies a natural crawl silhouette
            // without the absurd spectacle of a corpse continuing to paddle.
            super.setupAnim(entity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            hat.copyFrom(head);
        }
    }
}
