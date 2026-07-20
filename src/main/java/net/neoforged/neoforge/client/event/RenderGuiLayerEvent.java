package net.neoforged.neoforge.client.event;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
public class RenderGuiLayerEvent extends Event {
    private final ResourceLocation name;
    protected RenderGuiLayerEvent(ResourceLocation name){this.name=name;}
    public ResourceLocation getName(){return name;}
    public static final class Pre extends RenderGuiLayerEvent { public Pre(ResourceLocation name){super(name);} }
}
