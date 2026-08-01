package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Dynamically suppresses vanilla experience-orb rendering with the XP HUD option. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExperienceOrbPresentationModEvents {
    private ExperienceOrbPresentationModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.EXPERIENCE_ORB,
                ConditionalExperienceOrbRenderer::new);
    }

    private static final class ConditionalExperienceOrbRenderer
            extends ExperienceOrbRenderer {
        private ConditionalExperienceOrbRenderer(
                EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public void render(ExperienceOrb orb, float entityYaw,
                float partialTick, PoseStack poseStack,
                MultiBufferSource buffer, int packedLight) {
            if (InventoryModuleRuntimeState.disableExperienceBarForClient()) {
                return;
            }
            super.render(orb, entityYaw, partialTick, poseStack,
                    buffer, packedLight);
        }
    }
}
