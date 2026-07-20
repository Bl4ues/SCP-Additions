package net.mcreator.scpadditions.fabric;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
public final class FabricHudLayerRegistry {
    private record Entry(ResourceLocation id, RegisterGuiLayersEvent.Layer layer) {}
    private static final List<Entry> BELOW=new ArrayList<>(), ABOVE=new ArrayList<>();
    private FabricHudLayerRegistry(){}
    public static void registerBelow(ResourceLocation id, RegisterGuiLayersEvent.Layer layer){BELOW.add(new Entry(id,layer));}
    public static void registerAbove(ResourceLocation id, RegisterGuiLayersEvent.Layer layer){ABOVE.add(new Entry(id,layer));}
    static void render(GuiGraphics graphics, DeltaTracker tracker){
        for(Entry entry:BELOW) entry.layer.render(graphics,tracker);
        for(Entry entry:ABOVE) entry.layer.render(graphics,tracker);
    }
}
