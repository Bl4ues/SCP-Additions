package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;

import java.util.UUID;

/**
 * Prevents Minecraft's short vanilla death-body render from overlapping the
 * server-owned physical corpse.
 *
 * <p>The presence of a matching corpse is the authority signal instead of the
 * client's local module config. That makes multiplayer correct even when the
 * server controls whether physical bodies exist.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class PlayerCorpseRenderEvents {
    private PlayerCorpseRenderEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || player.isAlive() || player.level() == null) return;

        UUID owner = player.getUUID();
        AABB search = player.getBoundingBox().inflate(5.0D);
        boolean physicalBodyExists = !player.level().getEntitiesOfClass(
                PlayerCorpseEntity.class, search,
                corpse -> !corpse.isRemoved() && owner.equals(corpse.ownerId()))
                .isEmpty();
        if (physicalBodyExists) {
            event.setCanceled(true);
        }
    }
}
