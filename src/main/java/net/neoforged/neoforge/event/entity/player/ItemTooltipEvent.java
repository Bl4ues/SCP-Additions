package net.neoforged.neoforge.event.entity.player;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class ItemTooltipEvent extends Event {
    private final ItemStack stack; private final List<Component> tooltip;
    public ItemTooltipEvent(ItemStack stack, List<Component> tooltip) { this.stack = stack; this.tooltip = tooltip; }
    public ItemStack getItemStack() { return stack; }
    public List<Component> getToolTip() { return tooltip; }
}
