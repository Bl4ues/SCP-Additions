package net.mcreator.scpadditions.vitals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/**
 * Compatibility bridge for the standalone Inventory's NO_STAMINA item effect.
 *
 * Vanilla hand/armor items can be registered immediately. The future SCP
 * Inventory capability registers an additional equipped-item source here, so
 * the stamina controller does not need to know capability implementation
 * details or be rewritten when custom equipment arrives.
 */
public final class StaminaBlockerAccess {
    private static final Set<ResourceLocation> BLOCKING_ITEMS = new CopyOnWriteArraySet<>();
    private static final CopyOnWriteArrayList<Predicate<Player>> EXTRA_EQUIPPED_SOURCES =
            new CopyOnWriteArrayList<>();

    private StaminaBlockerAccess() {
    }

    public static boolean isBlocked(Player player) {
        if (player == null) {
            return false;
        }

        if (isBlockingStack(player.getMainHandItem())
                || isBlockingStack(player.getOffhandItem())) {
            return true;
        }

        for (ItemStack armor : player.getArmorSlots()) {
            if (isBlockingStack(armor)) {
                return true;
            }
        }

        for (Predicate<Player> source : EXTRA_EQUIPPED_SOURCES) {
            try {
                if (source.test(player)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // One optional integration must not disable the stamina system.
            }
        }
        return false;
    }

    public static boolean isBlockingStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && BLOCKING_ITEMS.contains(id);
    }

    public static Runnable registerBlockingItem(ResourceLocation itemId) {
        if (itemId == null) {
            return () -> { };
        }
        BLOCKING_ITEMS.add(itemId);
        return () -> BLOCKING_ITEMS.remove(itemId);
    }

    public static Runnable registerEquippedSource(Predicate<Player> source) {
        if (source == null) {
            return () -> { };
        }
        EXTRA_EQUIPPED_SOURCES.add(source);
        return () -> EXTRA_EQUIPPED_SOURCES.remove(source);
    }
}
