package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.block.entity.SystemTerminalBlockEntity;
import software.bernie.geckolib.model.GeoModel;

/** Resource binding for the ARC-Site-48 SCiPNET terminal. */
public final class SystemTerminalGeoModel
        extends GeoModel<SystemTerminalBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            "scp_additions", "geo/block/system_terminal.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "scp_additions", "textures/block/system_terminal.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            "scp_additions",
            "animations/block/system_terminal.animation.json");

    @Override
    public ResourceLocation getModelResource(
            SystemTerminalBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(
            SystemTerminalBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            SystemTerminalBlockEntity animatable) {
        return ANIMATION;
    }
}
