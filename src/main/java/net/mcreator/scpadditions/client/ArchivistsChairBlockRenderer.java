package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.mcreator.scpadditions.facility.ArchivistsChairBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class ArchivistsChairBlockRenderer
        extends GeoBlockRenderer<ArchivistsChairBlockEntity> {
    public ArchivistsChairBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new ArchivistsChairBlockModel());
    }
}
