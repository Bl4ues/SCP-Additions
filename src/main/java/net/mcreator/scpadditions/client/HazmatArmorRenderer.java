package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.item.HazmatArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class HazmatArmorRenderer extends GeoArmorRenderer<HazmatArmorItem> {
    public HazmatArmorRenderer() {
        super(new HazmatArmorModel());
    }
}
