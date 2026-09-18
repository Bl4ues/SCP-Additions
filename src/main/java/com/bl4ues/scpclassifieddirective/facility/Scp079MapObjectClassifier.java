package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModTabs;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Curated identification of non-entity SCP objects shown on SCP-079's map.
 * The Anomalies/SCP creative subsection is authoritative for portable SCP
 * items; physical-only states and temporary block variants are normalized here.
 */
public final class Scp079MapObjectClassifier {
    private static volatile Map<Item, Integer> itemNumbers;

    private Scp079MapObjectClassifier() {
    }

    public static int itemNumber(ItemStack stack) {
        if (stack == null || stack.isEmpty()
                || stack.getItem() instanceof SpawnEggItem) return -1;
        return itemNumbers().getOrDefault(stack.getItem(), -1);
    }

    public static int blockNumber(BlockState state, BlockEntity blockEntity) {
        if (state == null || state.isAir()) return -1;

        if (state.is(Scp714Items.SCP_714_PLACED.get())) return 714;
        if (state.is(Scp1576Module.PLACED_BLOCK.get())) return 1576;
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_294_STOCKING.get())
                || state.is(ScpClassifiedDirectiveModBlocks
                        .SCP_294_OUT_OF_RANGE.get())) {
            return 294;
        }
        if (state.is(ScpClassifiedDirectiveModBlocks.SCP_902_OPEN.get())) {
            return 902;
        }
        if (blockEntity instanceof Scp714ContainmentStandModule.StandBlockEntity
                stand && stand.hasRing()) {
            return 714;
        }

        int number = itemNumbers().getOrDefault(state.getBlock().asItem(), -1);
        // SCP-079 already has its dedicated physical-host marker.
        return number == 79 ? -1 : number;
    }

    public static boolean mayContainObject(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(Scp714ContainmentStandModule.BLOCK.get())
                || state.is(Scp714Items.SCP_714_PLACED.get())
                || state.is(Scp1576Module.PLACED_BLOCK.get())
                || state.is(ScpClassifiedDirectiveModBlocks
                        .SCP_294_STOCKING.get())
                || state.is(ScpClassifiedDirectiveModBlocks
                        .SCP_294_OUT_OF_RANGE.get())
                || state.is(ScpClassifiedDirectiveModBlocks
                        .SCP_902_OPEN.get())) {
            return true;
        }
        int number = itemNumbers().getOrDefault(state.getBlock().asItem(), -1);
        return number > 0 && number != 79;
    }

    private static Map<Item, Integer> itemNumbers() {
        Map<Item, Integer> cached = itemNumbers;
        if (cached != null) return cached;
        synchronized (Scp079MapObjectClassifier.class) {
            cached = itemNumbers;
            if (cached != null) return cached;
            IdentityHashMap<Item, Integer> built = new IdentityHashMap<>();
            for (ItemStack stack : ScpClassifiedDirectiveModTabs.scpStacks()) {
                if (stack == null || stack.isEmpty()
                        || stack.getItem() instanceof SpawnEggItem) continue;
                int number = parseScpNumber(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (number > 0) built.put(stack.getItem(), number);
            }
            itemNumbers = Map.copyOf(built);
            return itemNumbers;
        }
    }

    private static int parseScpNumber(ResourceLocation id) {
        if (id == null) return -1;
        String path = id.getPath();
        int start = path.indexOf("scp_");
        if (start < 0) return -1;
        start += 4;
        int end = start;
        while (end < path.length() && Character.isDigit(path.charAt(end))) {
            end++;
        }
        if (end == start) return -1;
        try {
            return Integer.parseInt(path.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
