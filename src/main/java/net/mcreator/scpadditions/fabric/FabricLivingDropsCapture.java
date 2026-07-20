package net.mcreator.scpadditions.fabric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Captures vanilla death item entities until the mutable NeoForge-style event is applied. */
public final class FabricLivingDropsCapture {
    private static final ThreadLocal<Deque<Context>> CONTEXTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private FabricLivingDropsCapture() {}

    public static void begin(LivingEntity entity, ServerLevel level) {
        CONTEXTS.get().push(new Context(entity, level, new ArrayList<>()));
    }

    public static boolean capture(ServerLevel level, Entity entity) {
        Deque<Context> stack = CONTEXTS.get();
        if (stack.isEmpty() || !(entity instanceof ItemEntity item)) {
            return false;
        }
        Context context = stack.peek();
        if (context.level != level) {
            return false;
        }
        context.drops.add(item);
        return true;
    }

    public static void finish(LivingEntity entity, ServerLevel level) {
        Deque<Context> stack = CONTEXTS.get();
        if (stack.isEmpty()) return;
        Context context = stack.pop();
        if (stack.isEmpty()) CONTEXTS.remove();
        if (context.entity != entity || context.level != level) {
            throw new IllegalStateException("Mismatched SCP Additions death-drop capture context");
        }
        LivingDropsEvent event = new LivingDropsEvent(entity, context.drops);
        NeoForge.EVENT_BUS.post(event);
        List<ItemEntity> finalDrops = List.copyOf(event.getDrops());
        for (ItemEntity drop : finalDrops) {
            if (drop != null && !drop.isRemoved() && !drop.getItem().isEmpty()) {
                level.addFreshEntity(drop);
            }
        }
    }

    private record Context(
            LivingEntity entity,
            ServerLevel level,
            List<ItemEntity> drops) {}
}
