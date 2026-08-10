package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.facility.ArchivistsChairBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class ArchivistsChairItemRenderer
        extends GeoItemRenderer<ArchivistsChairBlockItem> {
    public ArchivistsChairItemRenderer() {
        super(new ArchivistsChairItemModel());
    }
}
