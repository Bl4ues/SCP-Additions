package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.mcreator.scpadditions.scp1576.Scp1576PlacedBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class Scp1576PlacedBlockRenderer
        extends GeoBlockRenderer<Scp1576PlacedBlockEntity> {
    public Scp1576PlacedBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new Scp1576PlacedGeoModel());
    }
}
