package com.bl4ues.scpclassifieddirective.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Appends save/death compatibility packets after the core SCP registrations. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SaveGameNetwork {
    private static boolean registered;

    private SaveGameNetwork() {
    }

    @SubscribeEvent
    public static synchronized void onCommonSetup(FMLCommonSetupEvent event) {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(QuickSavePacket.class,
                QuickSavePacket::encode, QuickSavePacket::decode,
                QuickSavePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SaveStatePacket.class,
                SaveStatePacket::encode, SaveStatePacket::decode,
                SaveStatePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MineZeroDeathStatePacket.class,
                MineZeroDeathStatePacket::encode,
                MineZeroDeathStatePacket::decode,
                MineZeroDeathStatePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MineZeroLoadVotePacket.class,
                MineZeroLoadVotePacket::encode,
                MineZeroLoadVotePacket::decode,
                MineZeroLoadVotePacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MineZeroRestoreTransitionPacket.class,
                MineZeroRestoreTransitionPacket::encode,
                MineZeroRestoreTransitionPacket::decode,
                MineZeroRestoreTransitionPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MineZeroCompatibilityStatusPacket.class,
                MineZeroCompatibilityStatusPacket::encode,
                MineZeroCompatibilityStatusPacket::decode,
                MineZeroCompatibilityStatusPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(MineZeroCompatibilityRequestPacket.class,
                MineZeroCompatibilityRequestPacket::encode,
                MineZeroCompatibilityRequestPacket::decode,
                MineZeroCompatibilityRequestPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DeathSpectateRequestPacket.class,
                DeathSpectateRequestPacket::encode,
                DeathSpectateRequestPacket::decode,
                DeathSpectateRequestPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DeathSpectateStatePacket.class,
                DeathSpectateStatePacket::encode,
                DeathSpectateStatePacket::decode,
                DeathSpectateStatePacket::handle);
        // Keep new save/death packets appended so established message IDs do not move.
        ScpClassifiedDirectiveMod.addNetworkMessage(SaveFeedbackPacket.class,
                SaveFeedbackPacket::encode, SaveFeedbackPacket::decode,
                SaveFeedbackPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SimpleVoiceChatCompatibilityStatusPacket.class,
                SimpleVoiceChatCompatibilityStatusPacket::encode,
                SimpleVoiceChatCompatibilityStatusPacket::decode,
                SimpleVoiceChatCompatibilityStatusPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SimpleVoiceChatCompatibilityRequestPacket.class,
                SimpleVoiceChatCompatibilityRequestPacket::encode,
                SimpleVoiceChatCompatibilityRequestPacket::decode,
                SimpleVoiceChatCompatibilityRequestPacket::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DeathVoiceRosterPacket.class,
                DeathVoiceRosterPacket::encode,
                DeathVoiceRosterPacket::decode,
                DeathVoiceRosterPacket::handle);
    }
}
