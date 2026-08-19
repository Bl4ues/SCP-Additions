package net.mcreator.scpadditions.compat;

import net.minecraftforge.fml.ModList;

/** Runtime detection kept free of Simple Voice Chat API classes. */
public final class SimpleVoiceChatPresence {
    public static final String MOD_ID = "voicechat";
    public static final String API_MOD_ID = "voicechat_api";

    private SimpleVoiceChatPresence() {
    }

    public static boolean installed() {
        return ModList.get().isLoaded(MOD_ID)
                && ModList.get().isLoaded(API_MOD_ID);
    }
}
