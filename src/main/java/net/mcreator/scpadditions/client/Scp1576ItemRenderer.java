package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.scp1576.Scp1576Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class Scp1576ItemRenderer extends GeoItemRenderer<Scp1576Item> {
    public Scp1576ItemRenderer() {
        super(new Scp1576ItemGeoModel());
    }
}
