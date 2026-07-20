package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Scp173Keybinds {
    public static final KeyMapping BLINK = new KeyMapping(
            "key.scpinventory.blink",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.scpinventory"
    );

    private Scp173Keybinds() {
    }
}
