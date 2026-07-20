package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Scp131Keybinds {
    public static final KeyMapping DISMISS = new KeyMapping(
            "key.scpinventory.scp_131_dismiss",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.scpinventory"
    );

    private Scp131Keybinds() {
    }
}
