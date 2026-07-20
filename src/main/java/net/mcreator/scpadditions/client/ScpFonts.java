package net.mcreator.scpadditions.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Shared font resources migrated from SCP Inventory. */
public final class ScpFonts {
    public static final ResourceLocation ROBOTO = ResourceLocation.fromNamespaceAndPath("scpinventory", "roboto");
    public static final ResourceLocation MONTSERRAT = ResourceLocation.fromNamespaceAndPath("scpinventory", "montserrat");

    private ScpFonts() {
    }

    public static MutableComponent roboto(String text) {
        return Component.literal(text == null ? "" : text).withStyle(style -> style.withFont(ROBOTO));
    }

    public static MutableComponent roboto(Component component) {
        return Component.empty().append(component == null ? Component.empty() : component)
                .withStyle(style -> style.withFont(ROBOTO));
    }

    public static MutableComponent montserrat(String text) {
        return Component.literal(text == null ? "" : text).withStyle(style -> style.withFont(MONTSERRAT));
    }

    public static MutableComponent montserrat(Component component) {
        return Component.empty().append(component == null ? Component.empty() : component)
                .withStyle(style -> style.withFont(MONTSERRAT));
    }
}
