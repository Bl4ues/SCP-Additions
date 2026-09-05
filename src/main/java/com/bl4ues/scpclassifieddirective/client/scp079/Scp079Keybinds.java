package com.bl4ues.scpclassifieddirective.client.scp079;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Rebindable controls unique to playable SCP-079. */
public final class Scp079Keybinds {
    public static final KeyMapping USE_SPEAKER = new KeyMapping(
            "key.scp_classified_directive.scp_079_speaker",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.scp_classified_directive");
    public static final KeyMapping BLACKOUT = new KeyMapping(
            "key.scp_classified_directive.scp_079_blackout",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.scp_classified_directive");
    public static final KeyMapping LOCKDOWN = new KeyMapping(
            "key.scp_classified_directive.scp_079_lockdown",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.scp_classified_directive");

    private Scp079Keybinds() {
    }
}
