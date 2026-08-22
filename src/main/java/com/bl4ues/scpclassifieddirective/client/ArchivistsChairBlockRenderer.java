package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.bl4ues.scpclassifieddirective.facility.ArchivistsChairBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class ArchivistsChairBlockRenderer
        extends GeoBlockRenderer<ArchivistsChairBlockEntity> {
    public ArchivistsChairBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new ArchivistsChairBlockModel());
    }
}
