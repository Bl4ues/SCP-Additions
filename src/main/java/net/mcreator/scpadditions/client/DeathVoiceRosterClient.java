package net.mcreator.scpadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.DeathVoiceRosterPacket;

import java.util.List;

/** Client snapshot of dead players participating in the isolated voice call. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class DeathVoiceRosterClient {
    private static volatile List<DeathVoiceRosterPacket.Participant> participants =
            List.of();

    private DeathVoiceRosterClient() {
    }

    public static void receive(List<DeathVoiceRosterPacket.Participant> roster) {
        participants = roster == null ? List.of() : List.copyOf(roster);
    }

    public static List<DeathVoiceRosterPacket.Participant> participants() {
        return participants;
    }

    public static void clear() {
        participants = List.of();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
