package com.bl4ues.scpclassifieddirective.event;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative optional suppression for vanilla hostile mobs.
 *
 * Newly created vanilla monsters are rejected regardless of spawn source.
 * Hostiles that already existed when the module became active are removed,
 * except for named mobs, which remain protected across chunk reloads and
 * dimension transfers for the rest of that enabled session.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VanillaHostileMobControlEvents {
    private static final Map<MinecraftServer, Boolean> LAST_ENABLED =
            new WeakHashMap<>();
    private static final Map<MinecraftServer, Set<UUID>> NAMED_SURVIVORS =
            new WeakHashMap<>();

    private VanillaHostileMobControlEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !enabled()
                || !isVanillaHostile(event.getEntity())) {
            return;
        }

        Entity entity = event.getEntity();
        MinecraftServer server = level.getServer();
        Set<UUID> survivors = NAMED_SURVIVORS.computeIfAbsent(server,
                ignored -> new java.util.HashSet<>());

        if (survivors.contains(entity.getUUID())) {
            return;
        }

        // A named hostile loaded from an existing world predates this spawn
        // attempt and is explicitly exempt from the module.
        if (event.loadedFromDisk() && entity.hasCustomName()) {
            survivors.add(entity.getUUID());
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        boolean enabled = enabled();
        Boolean previous = LAST_ENABLED.put(server, enabled);

        if (!enabled) {
            NAMED_SURVIVORS.remove(server);
            return;
        }

        if (!Boolean.TRUE.equals(previous)) {
            removeExistingHostiles(server);
        }
    }

    private static void removeExistingHostiles(MinecraftServer server) {
        Set<UUID> survivors = NAMED_SURVIVORS.computeIfAbsent(server,
                ignored -> new java.util.HashSet<>());
        List<Entity> remove = new ArrayList<>();

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved() || !isVanillaHostile(entity)) continue;
                if (entity.hasCustomName()) {
                    survivors.add(entity.getUUID());
                } else {
                    remove.add(entity);
                }
            }
        }

        for (Entity entity : remove) {
            entity.discard();
        }
    }

    private static boolean enabled() {
        return ScpClassifiedDirectiveModulesConfig.get()
                .vanillaHostileMobs.enabled;
    }

    private static boolean isVanillaHostile(Entity entity) {
        if (!(entity instanceof Mob)
                || entity.getType().getCategory() != MobCategory.MONSTER) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(
                entity.getType());
        return id != null && "minecraft".equals(id.getNamespace());
    }
}
