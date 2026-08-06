package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.io.InputStream;

/**
 * Builds a non-destructive emissive texture from the terminal's authored
 * GeckoLib glowmask. GeckoLib 4's automatic texture mutates the shared base
 * texture, so item and block renderers instead share this generated overlay.
 */
public final class SystemTerminalGlowTexture {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/block/system_terminal.png");
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/block/system_terminal_glowmask.png");
    private static final ResourceLocation GENERATED = new ResourceLocation(
            ScpAdditionsMod.MODID, "generated/system_terminal_emissive");

    private static boolean registered;

    private SystemTerminalGlowTexture() {
    }

    /** Must be called on the render thread. */
    public static synchronized ResourceLocation texture() {
        if (!registered) register();
        return registered ? GENERATED : GLOWMASK;
    }

    private static void register() {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft.getResourceManager();

        try (InputStream baseStream = resources.getResourceOrThrow(BASE).open();
                InputStream maskStream = resources.getResourceOrThrow(GLOWMASK).open();
                NativeImage base = NativeImage.read(baseStream);
                NativeImage mask = NativeImage.read(maskStream)) {
            if (base.getWidth() != mask.getWidth()
                    || base.getHeight() != mask.getHeight()) {
                throw new IllegalStateException(
                        "Terminal base texture and glowmask dimensions differ");
            }

            NativeImage emissive = new NativeImage(
                    base.getWidth(), base.getHeight(), true);
            for (int y = 0; y < base.getHeight(); y++) {
                for (int x = 0; x < base.getWidth(); x++) {
                    int maskPixel = mask.getPixelRGBA(x, y);
                    int alpha = maskPixel >>> 24;
                    if (alpha == 0) {
                        emissive.setPixelRGBA(x, y, 0);
                    } else {
                        int basePixel = base.getPixelRGBA(x, y);
                        emissive.setPixelRGBA(x, y,
                                (basePixel & 0x00FFFFFF) | (alpha << 24));
                    }
                }
            }

            DynamicTexture texture = new DynamicTexture(emissive);
            minecraft.getTextureManager().register(GENERATED, texture);
            texture.upload();
            registered = true;
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not generate the Facility Diagnostic Terminal glow texture",
                    exception);
        }
    }
}
