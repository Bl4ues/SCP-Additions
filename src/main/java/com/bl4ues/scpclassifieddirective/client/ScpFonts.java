package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/** Shared font resources migrated from SCP Inventory. */
public final class ScpFonts {
    public static final ResourceLocation ROBOTO = new ResourceLocation("scp_classified_directive", "roboto");
    public static final ResourceLocation MONTSERRAT = new ResourceLocation("scp_classified_directive", "montserrat");
    public static final ResourceLocation LIBERATION_SANS_BOLD =
            new ResourceLocation("scp_classified_directive", "liberation_sans_bold");
    public static final ResourceLocation ANONYMOUS_PRO =
            new ResourceLocation("scp_classified_directive", "anonymous_pro");
    public static final ResourceLocation PF_VIDEOTEXT =
            new ResourceLocation("scp_classified_directive", "pf_videotext");
    public static final ResourceLocation JURA =
            new ResourceLocation("scp_classified_directive", "jura");
    public static final ResourceLocation NOTO_SANS_BOLD =
            new ResourceLocation("scp_classified_directive", "noto_sans_bold");
    public static final ResourceLocation TITILLIUM_WEB =
            new ResourceLocation("scp_classified_directive", "titillium_web");

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

    public static MutableComponent pfVideotext(String text) {
        return custom(text, PF_VIDEOTEXT);
    }

    public static MutableComponent doorSignNumbers(String text) {
        return custom(text, JURA);
    }

    public static MutableComponent scpSign(String text) {
        return custom(text, NOTO_SANS_BOLD);
    }

    public static MutableComponent titillium(String text) {
        return custom(text, TITILLIUM_WEB);
    }

    private static MutableComponent custom(String text, ResourceLocation font) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(font));
    }
}
