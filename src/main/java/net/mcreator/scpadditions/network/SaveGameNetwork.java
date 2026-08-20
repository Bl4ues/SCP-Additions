package net.mcreator.scpadditions.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Appends save/death compatibility packets after the core SCP registrations. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SaveGameNetwork {
    private static boolean registered;

    private SaveGameNetwork() {
    }

    @SubscribeEvent
    public static synchronized void onCommonSetup(FMLCommonSetupEvent event) {
        if (registered) return;
        registered = true;
        ScpAdditionsMod.addNetworkMessage(QuickSavePacket.class,
                QuickSavePacket::encode, QuickSavePacket::decode,
                QuickSavePacket::handle);
        ScpAdditionsMod.addNetworkMessage(SaveStatePacket.class,
                SaveStatePacket::encode, SaveStatePacket::decode,
                SaveStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(MineZeroDeathStatePacket.class,
                MineZeroDeathStatePacket::encode,
                MineZeroDeathStatePacket::decode,
                MineZeroDeathStatePacket::handle);
        ScpAdditionsMod.addNetworkMessage(MineZeroLoadVotePacket.class,
                MineZeroLoadVotePacket::encode,
                MineZeroLoadVotePacket::decode,
                MineZeroLoadVotePacket::handle);
        ScpAdditionsMod.addNetworkMessage(MineZeroRestoreTransitionPacket.class,
                MineZeroRestoreTransitionPacket::encode,
                MineZeroRestoreTransitionPacket::decode,
                MineZeroRestoreTransitionPacket::handle);
        ScpAdditionsMod.addNetworkMessage(MineZeroCompatibilityStatusPacket.class,
                MineZeroCompatibilityStatusPacket::encode,
                MineZeroCompatibilityStatusPacket::decode,
                MineZeroCompatibilityStatusPacket::handle);
        ScpAdditionsMod.addNetworkMessage(MineZeroCompatibilityRequestPacket.class,
                MineZeroCompatibilityRequestPacket::encode,
                MineZeroCompatibilityRequestPacket::decode,
                MineZeroCompatibilityRequestPacket::handle);
        ScpAdditionsMod.addNetworkMessage(DeathSpectateRequestPacket.class,
                DeathSpectateRequestPacket::encode,
                DeathSpectateRequestPacket::decode,
                DeathSpectateRequestPacket::handle);
        ScpAdditionsMod.addNetworkMessage(DeathSpectateStatePacket.class,
                DeathSpectateStatePacket::encode,
                DeathSpectateStatePacket::decode,
                DeathSpectateStatePacket::handle);
        // Keep new save/death packets appended so established message IDs do not move.
        ScpAdditionsMod.addNetworkMessage(SaveFeedbackPacket.class,
                SaveFeedbackPacket::encode, SaveFeedbackPacket::decode,
                SaveFeedbackPacket::handle);
        ScpAdditionsMod.addNetworkMessage(SimpleVoiceChatCompatibilityStatusPacket.class,
                SimpleVoiceChatCompatibilityStatusPacket::encode,
                SimpleVoiceChatCompatibilityStatusPacket::decode,
                SimpleVoiceChatCompatibilityStatusPacket::handle);
        ScpAdditionsMod.addNetworkMessage(SimpleVoiceChatCompatibilityRequestPacket.class,
                SimpleVoiceChatCompatibilityRequestPacket::encode,
                SimpleVoiceChatCompatibilityRequestPacket::decode,
                SimpleVoiceChatCompatibilityRequestPacket::handle);
        ScpAdditionsMod.addNetworkMessage(DeathVoiceRosterPacket.class,
                DeathVoiceRosterPacket::encode,
                DeathVoiceRosterPacket::decode,
                DeathVoiceRosterPacket::handle);
    }
}
