package net.mcreator.scpadditions.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CyclingCreativeTabIconItem extends Item {
    private static final long DISPLAY_TIME_MILLIS = 900L;

    private final Supplier<List<ItemStack>> stackSupplier;
    private volatile List<ItemStack> cachedStacks;

    public CyclingCreativeTabIconItem(Supplier<List<ItemStack>> stackSupplier) {
        super(new Item.Properties().stacksTo(1));
        this.stackSupplier = Objects.requireNonNull(stackSupplier);
    }

    public ItemStack currentDisplayStack() {
        List<ItemStack> stacks = cachedStacks;
        if (stacks == null) {
            stacks = stackSupplier.get().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            cachedStacks = stacks;
        }
        if (stacks.isEmpty()) return ItemStack.EMPTY;

        int index = (int) ((System.currentTimeMillis() / DISPLAY_TIME_MILLIS)
                % stacks.size());
        return stacks.get(index);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CyclingCreativeTabIconRenderer renderer;

            @Override
            public CyclingCreativeTabIconRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CyclingCreativeTabIconRenderer(
                            CyclingCreativeTabIconItem.this);
                }
                return renderer;
            }
        });
    }
}
