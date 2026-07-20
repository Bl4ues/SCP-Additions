package net.neoforged.neoforge.client.event;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
public class RegisterColorHandlersEvent extends Event {
    public static final class Block extends RegisterColorHandlersEvent {
        public void register(BlockColor provider, net.minecraft.world.level.block.Block... blocks) { ColorProviderRegistry.BLOCK.register(provider, blocks); }
    }
    public static final class Item extends RegisterColorHandlersEvent {
        public void register(ItemColor provider, ItemLike... items) { ColorProviderRegistry.ITEM.register(provider, items); }
    }
}
