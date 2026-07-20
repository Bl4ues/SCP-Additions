package net.neoforged.neoforge.client.event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.fabric.FabricHudLayerRegistry;
import net.neoforged.bus.api.Event;
public final class RegisterGuiLayersEvent extends Event {
    @FunctionalInterface public interface Layer { void render(GuiGraphics graphics, DeltaTracker tracker); }
    public void registerAboveAll(ResourceLocation id, Layer layer) { FabricHudLayerRegistry.registerAbove(id, layer); }
    public void registerBelowAll(ResourceLocation id, Layer layer) { FabricHudLayerRegistry.registerBelow(id, layer); }
}
