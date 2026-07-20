package net.neoforged.neoforge.event;
import java.util.function.Consumer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
public class BuildCreativeModeTabContentsEvent extends Event {
    private final CreativeModeTab tab; private final Consumer<ItemStack> output;
    public BuildCreativeModeTabContentsEvent(CreativeModeTab tab,Consumer<ItemStack> output){this.tab=tab;this.output=output;}
    public CreativeModeTab getTab(){return tab;}
    public void accept(ItemLike item){output.accept(new ItemStack(item));}
    public void accept(ItemStack stack){output.accept(stack);}
}
