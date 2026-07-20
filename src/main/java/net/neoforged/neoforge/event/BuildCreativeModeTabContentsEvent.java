package net.neoforged.neoforge.event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class BuildCreativeModeTabContentsEvent extends Event {
    private final ResourceKey<CreativeModeTab> key; private final CreativeModeTab tab;
    private final CreativeModeTab.Output output;
    public BuildCreativeModeTabContentsEvent(ResourceKey<CreativeModeTab> key, CreativeModeTab tab, CreativeModeTab.Output output) { this.key=key; this.tab=tab; this.output=output; }
    public ResourceKey<CreativeModeTab> getTabKey() { return key; }
    public CreativeModeTab getTab() { return tab; }
    public void accept(ItemLike item) { output.accept(item); }
    public void accept(ItemStack stack) { output.accept(stack); }
    public void remove(ItemStack stack, CreativeModeTab.TabVisibility visibility) {}
}
