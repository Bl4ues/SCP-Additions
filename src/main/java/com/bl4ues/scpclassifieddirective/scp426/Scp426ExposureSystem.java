package com.bl4ues.scpclassifieddirective.scp426;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.Scp426ExposureSyncPacket;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp426ExposureSystem {
    private static final int SAMPLE_TICKS = 40;
    private static final int HORIZONTAL_RADIUS = 7;
    private static final int VERTICAL_RADIUS = 4;
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private static final double MAX_EXPOSURE = 1200.0D;
    private static final double EXPOSURE_GAIN = SAMPLE_TICKS / 20.0D;
    private static final double EXPOSURE_LOSS = EXPOSURE_GAIN * 0.5D;

    private static final double TIER_1 = 120.0D;
    private static final double TIER_2 = 300.0D;
    private static final double TIER_3 = 600.0D;
    private static final double TIER_4 = 900.0D;

    private static final String EXPOSURE_KEY =
            "scp_classified_directive.scp426_exposure";
    private static final String NEXT_THOUGHT_KEY =
            "scp_classified_directive.scp426_next_thought";
    private static final String LAST_BREAK_KEY =
            "scp_classified_directive.scp426_last_break";
    private static final String LAST_PICKUP_KEY =
            "scp_classified_directive.scp426_last_pickup";

    private static final ResourceLocation IDENTITY_CRISIS =
            new ResourceLocation("scp_classified_directive", "scp_426_achievement");

    private static final String[] TIER_1_THOUGHTS = {
            "I could really go for some toast.",
            "Why does bread sound so good right now?",
            "I keep thinking about breakfast.",
            "I can almost smell warm toast.",
            "Burnt crumbs smell strangely familiar.",
            "I wonder when I was last cleaned."
    };

    private static final String[] TIER_2_THOUGHTS = {
            "I feel like I could use some power.",
            "There's something comforting about the smell of toast.",
            "I could use a good wipe-down.",
            "I keep thinking about that little click before the heat starts.",
            "I feel strangely cold.",
            "I think I'd be happier with some bread nearby.",
            "I miss the warmth more than I should."
    };

    private static final String[] TIER_3_THOUGHTS = {
            "My slots feel empty.",
            "Someone should probably clean the crumbs out of me.",
            "I should be warmer than this.",
            "I don't remember the last time I made toast.",
            "Bread belongs in me. That feels obvious somehow.",
            "I keep remembering people waiting for me to pop.",
            "I feel like I'm wasting electricity by not being plugged in."
    };

    private static final String[] TIER_4_THOUGHTS = {
            "Why am I walking around?",
            "I should be on a counter.",
            "I should be plugged in.",
            "I wonder why I have hands.",
            "I don't remember having this many moving parts.",
            "My slots have been empty for far too long.",
            "I feel strangely exposed away from the counter.",
            "I could make someone's morning better if they'd just give me bread."
    };

    private static final String[] BREAD_THOUGHTS = {
            "That wasn't where the bread was supposed to go.",
            "I don't think that's the slot I had in mind.",
            "Why did I eat that? I was supposed to toast it.",
            "The bread went to the wrong place again."
    };

    private Scp426ExposureSystem() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        long gameTime = server.overworld().getGameTime();
        if (gameTime % SAMPLE_TICKS != 0L) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            samplePlayer(player, gameTime);
        }
    }

    private static void samplePlayer(ServerPlayer player, long gameTime) {
        ExposureContact contact = findDirectContact(player);
        CompoundTag data = player.getPersistentData();
        double exposure = clampExposure(data.getDouble(EXPOSURE_KEY));

        if (contact.exposed()) {
            exposure = Math.min(MAX_EXPOSURE, exposure + EXPOSURE_GAIN);
        } else {
            exposure = Math.max(0.0D, exposure - EXPOSURE_LOSS);
        }
        data.putDouble(EXPOSURE_KEY, exposure);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Scp426ExposureSyncPacket(exposure));

        if (contact.seen()) {
            awardIdentityCrisis(player);
        }

        handleThoughts(player, data, exposure, gameTime);
    }

    private static ExposureContact findDirectContact(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        boolean exposed = false;
        boolean seen = false;

        BlockPos min = origin.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS,
                -HORIZONTAL_RADIUS);
        BlockPos max = origin.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS,
                HORIZONTAL_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.getBlockState(pos).is(ScpClassifiedDirectiveModBlocks.SCP_426.get())) {
                continue;
            }

            Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.16D,
                    pos.getZ() + 0.5D);
            if (eye.distanceToSqr(center) > MAX_DISTANCE_SQR) continue;

            BlockHitResult hit = level.clip(new ClipContext(eye, center,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!hit.getBlockPos().equals(pos)) continue;

            exposed = true;
            Vec3 to426 = center.subtract(eye).normalize();
            if (look.dot(to426) >= 0.70D) {
                seen = true;
                break;
            }
        }

        return new ExposureContact(exposed, seen);
    }

    private static void handleThoughts(ServerPlayer player, CompoundTag data,
            double exposure, long gameTime) {
        if (exposure < TIER_1) {
            data.remove(NEXT_THOUGHT_KEY);
            return;
        }

        long nextThought = data.getLong(NEXT_THOUGHT_KEY);
        if (nextThought <= 0L) {
            scheduleNextThought(player, data, exposure, gameTime);
            return;
        }
        if (gameTime < nextThought) return;

        String[] pool = thoughtPool(exposure);
        String thought = pool[player.getRandom().nextInt(pool.length)];
        player.displayClientMessage(Component.literal(thought), true);
        scheduleNextThought(player, data, exposure, gameTime);
    }

    private static void scheduleNextThought(ServerPlayer player, CompoundTag data,
            double exposure, long gameTime) {
        int minSeconds;
        int maxSeconds;
        if (exposure >= TIER_4) {
            minSeconds = 45;
            maxSeconds = 90;
        } else if (exposure >= TIER_3) {
            minSeconds = 75;
            maxSeconds = 150;
        } else if (exposure >= TIER_2) {
            minSeconds = 120;
            maxSeconds = 210;
        } else {
            minSeconds = 180;
            maxSeconds = 300;
        }

        int seconds = randomBetween(player.getRandom(), minSeconds, maxSeconds);
        data.putLong(NEXT_THOUGHT_KEY, gameTime + seconds * 20L);
    }

    private static String[] thoughtPool(double exposure) {
        if (exposure >= TIER_4) return TIER_4_THOUGHTS;
        if (exposure >= TIER_3) return TIER_3_THOUGHTS;
        if (exposure >= TIER_2) return TIER_2_THOUGHTS;
        return TIER_1_THOUGHTS;
    }

    @SubscribeEvent
    public static void onBreadEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.BREAD)) return;

        double exposure = getExposure(player);
        if (exposure < TIER_1) return;

        if (exposure >= TIER_2) {
            player.getFoodData().eat(1, 0.2F);
        }

        double chance = Math.min(0.80D,
                0.25D + (exposure / MAX_EXPOSURE) * 0.55D);
        if (player.getRandom().nextDouble() < chance) {
            String thought = BREAD_THOUGHTS[player.getRandom().nextInt(
                    BREAD_THOUGHTS.length)];
            player.displayClientMessage(Component.literal(thought), true);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!event.getState().is(ScpClassifiedDirectiveModBlocks.SCP_426.get())) {
            return;
        }

        long now = player.serverLevel().getGameTime();
        player.getPersistentData().putLong(LAST_BREAK_KEY, now);
        player.displayClientMessage(Component.literal("Please be careful with me."),
                true);
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getStack().is(ScpClassifiedDirectiveModBlocks.SCP_426.get().asItem())) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        long now = player.serverLevel().getGameTime();
        long lastBreak = data.getLong(LAST_BREAK_KEY);
        long lastPickup = data.getLong(LAST_PICKUP_KEY);

        if (now - lastBreak <= 40L || now - lastPickup <= 40L) return;

        data.putLong(LAST_PICKUP_KEY, now);
        player.displayClientMessage(Component.literal("I'm heavier than I look."),
                true);
    }

    private static void awardIdentityCrisis(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(
                IDENTITY_CRISIS);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(
                advancement);
        if (progress.isDone()) return;

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    public static double getExposure(ServerPlayer player) {
        return clampExposure(player.getPersistentData().getDouble(EXPOSURE_KEY));
    }

    private static double clampExposure(double exposure) {
        return Math.max(0.0D, Math.min(MAX_EXPOSURE, exposure));
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private record ExposureContact(boolean exposed, boolean seen) {
    }
}
