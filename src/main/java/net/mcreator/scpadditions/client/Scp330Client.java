package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.block.Scp330Block;
import net.mcreator.scpadditions.block.entity.Scp330BlockEntity;
import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;
import net.mcreator.scpadditions.scp330.Scp330Hands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class Scp330Client {
    private Scp330Client() {
    }

    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ScpAdditionsModBlockEntities.SCP_330.get(),
                    context -> new Renderer());
        }
    }

    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
    public static final class InteractionGuard {
        private InteractionGuard() {
        }

        @SubscribeEvent
        public static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
            if (Scp330Hands.isDisabled(Minecraft.getInstance().player)
                    && (event.isUseItem() || event.isAttack())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onScreenOpening(ScreenEvent.Opening event) {
            if (!Scp330Hands.isDisabled(Minecraft.getInstance().player)) return;
            if (event.getNewScreen() instanceof AbstractContainerScreen<?>
                    || event.getNewScreen() instanceof ScpInventoryScreen) {
                event.setCanceled(true);
            }
        }
    }

    private static final class Model extends GeoModel<Scp330BlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                ScpAdditionsMod.MODID, "geo/block/scp330.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpAdditionsMod.MODID, "textures/block/scp330.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpAdditionsMod.MODID, "animations/block/scp330.animation.json");

        @Override
        public ResourceLocation getModelResource(Scp330BlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(Scp330BlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Scp330BlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class Renderer extends GeoBlockRenderer<Scp330BlockEntity> {
        private Renderer() {
            super(new Model());
        }
    }
}
