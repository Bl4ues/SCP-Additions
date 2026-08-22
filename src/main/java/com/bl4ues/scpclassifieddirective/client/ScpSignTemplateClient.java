package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateSummary;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplates;
import com.bl4ues.scpclassifieddirective.network.ScpSignTemplateRequestPacket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-only dynamic textures and metadata synchronized from the world. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpSignTemplateClient {
    private static final Map<String, ScpSignTemplateSummary> CUSTOM =
            new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES =
            new LinkedHashMap<>();
    private static final Set<String> REQUESTED = new HashSet<>();

    private static int revision;
    private static String lastChangedId = "";

    private ScpSignTemplateClient() {
    }

    public static synchronized List<ScpSignTemplateSummary> options() {
        List<ScpSignTemplateSummary> result = new ArrayList<>(
                ScpSignTemplates.builtIns());
        result.addAll(CUSTOM.values());
        return List.copyOf(result);
    }

    public static synchronized int revision() {
        return revision;
    }

    public static synchronized String lastChangedId() {
        return lastChangedId;
    }

    public static synchronized void applyLibrary(
            List<ScpSignTemplateSummary> summaries, String changedId) {
        Set<String> retained = new HashSet<>();
        CUSTOM.clear();
        if (summaries != null) {
            for (ScpSignTemplateSummary summary : summaries) {
                if (summary != null && summary.custom()
                        && ScpSignTemplates.isCustom(summary.id())) {
                    CUSTOM.put(summary.id(), summary);
                    retained.add(summary.id());
                }
            }
        }

        List<String> stale = TEXTURES.keySet().stream()
                .filter(id -> !retained.contains(id)).toList();
        Minecraft minecraft = Minecraft.getInstance();
        for (String id : stale) {
            ResourceLocation texture = TEXTURES.remove(id);
            if (texture != null) minecraft.getTextureManager().release(texture);
            REQUESTED.remove(id);
        }
        lastChangedId = ScpSignTemplates.cleanId(changedId);
        revision++;
    }

    public static synchronized void acceptImage(String id, String name,
            byte[] png) {
        String cleanId = ScpSignTemplates.cleanId(id);
        if (!ScpSignTemplates.isCustom(cleanId) || png == null
                || png.length == 0) {
            return;
        }

        NativeImage image = null;
        try {
            image = NativeImage.read(new ByteArrayInputStream(png));
            if (image.getWidth() != ScpSignTemplates.TARGET_WIDTH
                    || image.getHeight() != ScpSignTemplates.TARGET_HEIGHT) {
                image.close();
                REQUESTED.remove(cleanId);
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            ResourceLocation old = TEXTURES.remove(cleanId);
            if (old != null) minecraft.getTextureManager().release(old);
            ResourceLocation registered = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID,
                    "world_sign_templates/"
                            + cleanId.substring(
                            ScpSignTemplates.CUSTOM_PREFIX.length()));
            minecraft.getTextureManager().register(registered,
                    new DynamicTexture(image));
            image = null;
            TEXTURES.put(cleanId, registered);
            CUSTOM.put(cleanId, new ScpSignTemplateSummary(cleanId, name,
                    true));
            REQUESTED.remove(cleanId);
            lastChangedId = cleanId;
            revision++;
        } catch (IOException | RuntimeException ignored) {
            if (image != null) image.close();
            REQUESTED.remove(cleanId);
        }
    }

    public static synchronized ResourceLocation texture(String id) {
        String clean = ScpSignTemplates.cleanId(id);
        ResourceLocation builtIn = ScpSignTemplates.builtInTexture(clean);
        if (builtIn != null) return builtIn;
        if (!ScpSignTemplates.isCustom(clean)) return null;
        ResourceLocation texture = TEXTURES.get(clean);
        // A player can encounter an already-placed custom sign before ever
        // opening its editor. Request by the block-entity ID itself; the server
        // remains authoritative and simply ignores unknown/deleted templates.
        if (texture == null && REQUESTED.add(clean)) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new ScpSignTemplateRequestPacket(clean));
        }
        return texture;
    }

    public static synchronized ScpSignTemplateSummary summary(String id) {
        String clean = ScpSignTemplates.cleanId(id);
        for (ScpSignTemplateSummary builtIn : ScpSignTemplates.builtIns()) {
            if (builtIn.id().equals(clean)) return builtIn;
        }
        return CUSTOM.get(clean);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static synchronized void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation texture : TEXTURES.values()) {
            minecraft.getTextureManager().release(texture);
        }
        TEXTURES.clear();
        CUSTOM.clear();
        REQUESTED.clear();
        lastChangedId = "";
        revision++;
    }
}
