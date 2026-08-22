package com.bl4ues.scpclassifieddirective.client.photomode;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Single feature gate for the developer-facing photo mode.
 *
 * <p>Set {@link #DEFAULT_ENABLED} to false when the tool should remain compiled
 * into a public build without being exposed. It can still be temporarily
 * enabled or disabled without rebuilding with the JVM property
 * {@code -Dscp_classified_directive.photoMode=true|false}.</p>
 */
public final class PhotoModeFeature {
    private static final boolean DEFAULT_ENABLED = true;

    /** Maximum distance at which the frozen-frame picker can select an object. */
    static final double PICK_DISTANCE = 96.0D;

    /** Transparent padding retained around the cropped object, in framebuffer pixels. */
    static final int OUTPUT_PADDING = 16;

    public static final KeyMapping OPEN_PHOTO_MODE = new KeyMapping(
            "Photo Mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "key.categories.scp_classified_directive"
    );

    private PhotoModeFeature() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(
                "scp_classified_directive.photoMode",
                Boolean.toString(DEFAULT_ENABLED)));
    }
}
