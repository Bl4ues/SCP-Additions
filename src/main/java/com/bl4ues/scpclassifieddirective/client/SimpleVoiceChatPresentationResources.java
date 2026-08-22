package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers SCP: Classified Directive' Simple Voice Chat presentation as a required built-in
 * resource pack. Mod resource ordering is not a reliable way to override assets
 * in another mod's namespace, so the dedicated pack is kept at the top of the
 * client resource-pack stack instead.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SimpleVoiceChatPresentationResources {
    private static final String VOICECHAT_MOD_ID = "voicechat";
    private static final String PACK_ID = "builtin/scp_classified_directive_voicechat";
    private static final String PACK_ROOT = "resourcepacks/scp_classified_directive_voicechat";

    private SimpleVoiceChatPresentationResources() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES
                || !ModList.get().isLoaded(VOICECHAT_MOD_ID)) {
            return;
        }

        var modFile = ModList.get().getModFileById(ScpClassifiedDirectiveMod.MODID);
        if (modFile == null) {
            return;
        }

        var resourcePath = modFile.getFile().findResource(PACK_ROOT);
        var pack = Pack.readMetaAndCreate(
                PACK_ID,
                Component.literal("SCP: Classified Directive - Simple Voice Chat Presentation"),
                true,
                id -> new PathPackResources(id, resourcePath, false),
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.BUILT_IN);

        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
