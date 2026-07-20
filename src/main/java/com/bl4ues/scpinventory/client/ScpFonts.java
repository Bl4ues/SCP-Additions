package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public final class ScpFonts {
    public static final ResourceLocation ROBOTO = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "roboto");
    public static final ResourceLocation MONTSERRAT = ResourceLocation.fromNamespaceAndPath(ScpInventoryMod.MODID, "montserrat");

    private ScpFonts() {
    }

    public static MutableComponent roboto(String text) {
        return Component.literal(text == null ? "" : text).withStyle(style -> style.withFont(ROBOTO));
    }

    public static MutableComponent roboto(Component component) {
        return Component.empty().append(component == null ? Component.empty() : component).withStyle(style -> style.withFont(ROBOTO));
    }

    public static MutableComponent montserrat(String text) {
        return Component.literal(text == null ? "" : text).withStyle(style -> style.withFont(MONTSERRAT));
    }
}
