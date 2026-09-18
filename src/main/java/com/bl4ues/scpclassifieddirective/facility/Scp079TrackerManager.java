package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp131AEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp131BEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.Scp079MapObjectClassifier;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative lifeform counts and SCP map markers for playable 079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079TrackerManager {
    private static int tick;

    private Scp079TrackerManager() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++tick % 10 != 0) return;
        ServerPlayer controller = Scp079PlayableManager.controller(
                event.getServer());
        if (controller == null) return;

        int targets = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isAlive() && !player.isSpectator()
                    && !Scp079PlayableManager.isController(player)) {
                targets++;
            }
        }

        List<Scp079PlayableNetwork.TrackerEntry> markers = new ArrayList<>();
        List<Scp079PlayableNetwork.ObjectMarkerEntry> objects =
                new ArrayList<>();
        int scpSubjects = 1; // the player-controlled SCP-079 itself
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                int number = scpNumber(entity);
                if (number < 0 || !entity.isAlive()) continue;
                scpSubjects++;
                FacilityRoom room = FacilityMappingManager.roomForPosition(
                        level, entity.blockPosition());
                if (room != null) {
                    markers.add(new Scp079PlayableNetwork.TrackerEntry(
                            level.dimension().location(), room.id(),
                            entity.getX(), entity.getZ(), entity.getYRot(),
                            number));
                }
            }

            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                    continue;
                }
                int number = Scp079MapObjectClassifier.itemNumber(item.getItem());
                if (number <= 0) continue;
                FacilityRoom room = FacilityMappingManager.roomForPosition(
                        level, item.blockPosition());
                if (room != null) {
                    objects.add(new Scp079PlayableNetwork.ObjectMarkerEntry(
                            level.dimension().location(), room.id(),
                            item.getX(), item.getZ(), number));
                }
            }

            for (Scp079FacilityAccessManager.MapScpObject object
                    : Scp079FacilityAccessManager.mapScpObjects(
                            event.getServer(),
                            level.dimension().location())) {
                FacilityRoom room = FacilityMappingManager.roomForPosition(
                        level, object.pos());
                if (room != null) {
                    objects.add(new Scp079PlayableNetwork.ObjectMarkerEntry(
                            level.dimension().location(), room.id(),
                            object.pos().getX() + 0.5D,
                            object.pos().getZ() + 0.5D,
                            object.number()));
                }
            }
        }
        int totalLifeforms = targets + scpSubjects;
        Scp079PlayableNetwork.sendTracking(controller, totalLifeforms,
                targets, scpSubjects, markers, objects);
    }

    private static int scpNumber(Entity entity) {
        if (entity instanceof Scp106Entity) return 106;
        if (entity instanceof Scp131AEntity || entity instanceof Scp131BEntity) {
            return 131;
        }
        if (entity instanceof Scp173Entity) return 173;
        if (entity instanceof Scp939Entity) return 939;
        return -1;
    }
}
