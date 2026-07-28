package net.mcreator.scpadditions.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Shared font resources migrated from SCP Inventory. */
public final class ScpFonts {
    public static final ResourceLocation ROBOTO = new ResourceLocation("scpinventory", "roboto");
    public static final ResourceLocation MONTSERRAT = new ResourceLocation("scpinventory", "montserrat");
    public static final ResourceLocation LIBERATION_SANS_BOLD =
            new ResourceLocation("scp_additions", "liberation_sans_bold");
    public static final ResourceLocation ANONYMOUS_PRO =
            new ResourceLocation("scp_additions", "anonymous_pro");
    public static final ResourceLocation JURA =
            new ResourceLocation("scp_additions", "jura");
    public static final ResourceLocation NOTO_SANS_BOLD =
            new ResourceLocation("scp_additions", "noto_sans_bold");

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

    public static MutableComponent liberationSans(String text) {
        return custom(text, LIBERATION_SANS_BOLD);
    }

    public static MutableComponent anonymousPro(String text) {
        return custom(text, ANONYMOUS_PRO);
    }

    public static MutableComponent doorSignNumbers(String text) {
        return custom(text, JURA);
    }

    public static MutableComponent scpSign(String text) {
        return custom(text, NOTO_SANS_BOLD);
    }

    private static MutableComponent custom(String text, ResourceLocation font) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(font));
    }
}
