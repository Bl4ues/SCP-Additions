package net.mcreator.scpadditions.death;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.DeathVoiceRosterPacket;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Comparator;
import java.util.List;

/** Periodically supplies dead clients with the membership of their voice call. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathVoiceRosterServerEvents {
    private static int refreshTick;

    private DeathVoiceRosterServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++refreshTick % 20 != 0) {
            return;
        }

        List<ServerPlayer> dead = DeathSpectateCoordinator
                .deadVoiceParticipants(event.getServer());
        if (dead.isEmpty()) return;

        List<DeathVoiceRosterPacket.Participant> roster = dead.stream()
                .sorted(Comparator.comparing(player ->
                        player.getGameProfile().getName().toLowerCase()))
                .map(player -> new DeathVoiceRosterPacket.Participant(
                        player.getUUID(), player.getGameProfile().getName(),
                        skinOverride(player)))
                .toList();
        DeathVoiceRosterPacket packet = new DeathVoiceRosterPacket(roster);

        for (ServerPlayer observer : dead) {
            if (observer.connection == null) continue;
            ScpAdditionsMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> observer), packet);
        }
    }

    private static String skinOverride(ServerPlayer player) {
        ScpAdditionsModVariables.PlayerVariables variables = player
                .getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        return variables == null || variables.scp914Skin == null
                ? "" : variables.scp914Skin;
    }
}
