package com.bl4ues.scpclassifieddirective.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Small server-side helper for SCP: Classified Directive advancement triggers. */
public final class ScpAdvancementAwards {
    public static final ResourceLocation FROM_THE_TRENCHES = id("from_the_trenches");
    public static final ResourceLocation TESLA = id("tesla");
    public static final ResourceLocation SWEET_TOOTH = id("scp_330_achievement");
    public static final ResourceLocation EYES_ON_ME = id("eyes_on_me");
    public static final ResourceLocation WHAT = id("what");
    public static final ResourceLocation CONCRETE_AND_REBAR = id("concrete_and_rebar");

    private ScpAdvancementAwards() {
    }

    public static void award(ServerPlayer player, ResourceLocation id) {
        if (player == null || id == null || player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(id);
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }
}
