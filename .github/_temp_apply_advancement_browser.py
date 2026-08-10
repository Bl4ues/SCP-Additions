from pathlib import Path
import json


def read(path):
    return Path(path).read_text()


def write(path, text):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text)


def replace_exact(path, old, new, count=1):
    text = read(path)
    actual = text.count(old)
    if actual != count:
        raise SystemExit(
            f'{path}: expected {count} occurrence(s), found {actual}: {old[:100]!r}')
    write(path, text.replace(old, new, count))


# Chat: move only the EditBox text down. Keep the frame and > prompt at their
# original absolute coordinates.
chat_layout = 'src/main/java/net/mcreator/scpadditions/client/FacilityChatLayout.java'
replace_exact(chat_layout,
    '    private static final int INPUT_LEFT = 17;\n',
    '    private static final int INPUT_LEFT = 17;\n'
    '    public static final int INPUT_TEXT_OFFSET = 3;\n')
replace_exact(chat_layout,
    '    public static int suggestionTop(EditBox input, int screenHeight, int lineLimit) {\n'
    '        int desired = input.getY() + input.getHeight() + 4;\n',
    '    public static int suggestionTop(EditBox input, int screenHeight, int lineLimit) {\n'
    '        int frameY = input.getY() - INPUT_TEXT_OFFSET;\n'
    '        int desired = frameY + input.getHeight() + 4;\n')
replace_exact(chat_layout,
    '        int top = input.getY() - 4;\n'
    '        int bottom = input.getY() + input.getHeight();\n',
    '        int frameY = input.getY() - INPUT_TEXT_OFFSET;\n'
    '        int top = frameY - 2;\n'
    '        int bottom = frameY + input.getHeight() + 2;\n')
replace_exact(chat_layout,
    '                7, input.getY() + 2, PALE_GOLD, false);\n',
    '                7, frameY + 2, PALE_GOLD, false);\n')

chat_mixin = 'src/main/java/net/mcreator/scpadditions/mixin/client/ChatScreenMixin.java'
replace_exact(chat_mixin,
    '        this.input.setY(FacilityChatLayout.inputY(chat)\n'
    '                + FacilityChatLayout.openOffsetScreen(chat));\n',
    '        this.input.setY(FacilityChatLayout.inputY(chat)\n'
    '                + FacilityChatLayout.openOffsetScreen(chat)\n'
    '                + FacilityChatLayout.INPUT_TEXT_OFFSET);\n', 2)

# Chair: the rendered/model forward direction is opposite the old collision
# facing convention. Correct the orientation instead of accumulating offsets.
chair = 'src/main/java/net/mcreator/scpadditions/facility/ArchivistsChairBlock.java'
replace_exact(chair,
    '        return switch (facing) {\n'
    '            case EAST -> EAST;\n'
    '            case SOUTH -> SOUTH;\n'
    '            case WEST -> WEST;\n'
    '            default -> NORTH;\n'
    '        };\n',
    '        // GeckoLib renders this authored model with the opposite\n'
    '        // horizontal forward convention from the vanilla block-facing\n'
    '        // collision transform. Mirror the facing by 180 degrees so the\n'
    '        // collision sits on the visible chair instead of across the\n'
    '        // placement origin.\n'
    '        return switch (facing) {\n'
    '            case EAST -> WEST;\n'
    '            case SOUTH -> NORTH;\n'
    '            case WEST -> EAST;\n'
    '            default -> SOUTH;\n'
    '        };\n')

# Shared server-side advancement award helper.
write('src/main/java/net/mcreator/scpadditions/advancement/ScpAdvancementAwards.java', '''package net.mcreator.scpadditions.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Small server-side helper for SCP Additions advancement triggers. */
public final class ScpAdvancementAwards {
    public static final ResourceLocation FROM_THE_TRENCHES = id("from_the_trenches");
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
        return new ResourceLocation(ScpAdditionsMod.MODID, path);
    }
}
''')

# SCP-106: surviving a managed hunt includes letting its interest expire or
# deliberately repelling it with a Tesla Gate while the hunted player is alive.
scp106 = 'src/main/java/net/mcreator/scpadditions/entity/Scp106Entity.java'
replace_exact(scp106,
    'import net.mcreator.scpadditions.facility.FacilityModule;\n',
    'import net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n'
    'import net.mcreator.scpadditions.facility.FacilityModule;\n')
replace_exact(scp106,
    '    private void beginVanish(boolean despawnAfterward) {\n'
    '        if (getEncounterState() == VANISHING) return;\n'
    '        vanishForDespawn = despawnAfterward;\n',
    '    private void beginVanish(boolean despawnAfterward) {\n'
    '        if (getEncounterState() == VANISHING) return;\n'
    '        if (despawnAfterward) awardManagedEncounterSurvival();\n'
    '        vanishForDespawn = despawnAfterward;\n')
replace_exact(scp106,
    '    private void tickVanish() {\n',
    '    private void awardManagedEncounterSurvival() {\n'
    '        if (!managedEncounter || huntedPlayerId == null\n'
    '                || !(level() instanceof ServerLevel serverLevel)) {\n'
    '            return;\n'
    '        }\n'
    '        ServerPlayer player = serverLevel.getServer().getPlayerList()\n'
    '                .getPlayer(huntedPlayerId);\n'
    '        if (player != null && isValidHuntTarget(player)) {\n'
    '            ScpAdvancementAwards.award(player,\n'
    '                    ScpAdvancementAwards.FROM_THE_TRENCHES);\n'
    '        }\n'
    '    }\n\n'
    '    private void tickVanish() {\n')

# SCP-173 routine encounter survival tracking.
scp173 = 'src/main/java/net/mcreator/scpadditions/entity/Scp173Entity.java'
replace_exact(scp173,
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n',
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n'
    'import net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n')
replace_exact(scp173,
    '    private double frozenFallSpeed;\n',
    '    private double frozenFallSpeed;\n'
    '    private UUID routineEncounterPlayerId;\n')
replace_exact(scp173,
    '    public void markRoutineSpawn() {\n'
    '        entityData.set(ROUTINE_SPAWN, true);\n',
    '    public void markRoutineSpawn() {\n'
    '        markRoutineSpawn(null);\n'
    '    }\n\n'
    '    public void markRoutineSpawn(ServerPlayer player) {\n'
    '        routineEncounterPlayerId = player == null\n'
    '                ? null : player.getUUID();\n'
    '        entityData.set(ROUTINE_SPAWN, true);\n')
replace_exact(scp173,
    '        tag.putInt("LastSeenOrCloseTick", lastSeenOrCloseTick);\n',
    '        tag.putInt("LastSeenOrCloseTick", lastSeenOrCloseTick);\n'
    '        if (routineEncounterPlayerId != null) {\n'
    '            tag.putUUID("RoutineEncounterPlayer",\n'
    '                    routineEncounterPlayerId);\n'
    '        }\n')
replace_exact(scp173,
    '        lastSeenOrCloseTick = tag.getInt("LastSeenOrCloseTick");\n',
    '        lastSeenOrCloseTick = tag.getInt("LastSeenOrCloseTick");\n'
    '        routineEncounterPlayerId = tag.hasUUID("RoutineEncounterPlayer")\n'
    '                ? tag.getUUID("RoutineEncounterPlayer") : null;\n')
replace_exact(scp173,
    '    private void handleRoutineDespawn() {\n'
    '        if (!isRoutineSpawn() || level().isClientSide) return;\n'
    '        if (findObservingPlayer() != null || hasClientObservationLock() || hasClosePlayer()) return;\n'
    '        if (tickCount - lastSeenOrCloseTick >= ROUTINE_DESPAWN_UNSEEN_TICKS) discard();\n'
    '    }\n',
    '    private void handleRoutineDespawn() {\n'
    '        if (!isRoutineSpawn() || level().isClientSide) return;\n'
    '        if (findObservingPlayer() != null || hasClientObservationLock() || hasClosePlayer()) return;\n'
    '        if (tickCount - lastSeenOrCloseTick >= ROUTINE_DESPAWN_UNSEEN_TICKS) {\n'
    '            completeRoutineEncounter();\n'
    '            discard();\n'
    '        }\n'
    '    }\n\n'
    '    public void completeRoutineEncounter() {\n'
    '        if (!isRoutineSpawn() || !isActivated()\n'
    '                || routineEncounterPlayerId == null\n'
    '                || level().isClientSide) {\n'
    '            return;\n'
    '        }\n'
    '        ServerPlayer player = getServer() == null ? null\n'
    '                : getServer().getPlayerList()\n'
    '                .getPlayer(routineEncounterPlayerId);\n'
    '        if (player != null && player.isAlive()\n'
    '                && !player.isCreative() && !player.isSpectator()) {\n'
    '            ScpAdvancementAwards.award(player,\n'
    '                    ScpAdvancementAwards.CONCRETE_AND_REBAR);\n'
    '        }\n'
    '        routineEncounterPlayerId = null;\n'
    '    }\n')

spawn173 = 'src/main/java/net/mcreator/scpadditions/event/Scp173SpawnEvents.java'
replace_exact(spawn173,
    '        scp173.markRoutineSpawn();\n',
    '        scp173.markRoutineSpawn(player);\n')

access173 = 'src/main/java/net/mcreator/scpadditions/entity/Scp173AccessDespawnController.java'
replace_exact(access173,
    '            if (gameTime - since >= NO_ACCESS_DESPAWN_TICKS) {\n'
    '                scp173.discard();\n',
    '            if (gameTime - since >= NO_ACCESS_DESPAWN_TICKS) {\n'
    '                scp173.completeRoutineEncounter();\n'
    '                scp173.discard();\n')

# SCP-131 hidden achievement at the actual stop-and-watch moment.
scp131 = 'src/main/java/net/mcreator/scpadditions/entity/AbstractScp131Entity.java'
replace_exact(scp131,
    'import net.mcreator.scpadditions.network.ScpEntityNetwork;\n',
    'import net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n'
    'import net.mcreator.scpadditions.network.ScpEntityNetwork;\n')
replace_exact(scp131,
    '    private boolean wasWatchingScp173;\n',
    '    private boolean wasWatchingScp173;\n'
    '    private UUID scp173WatchWitness;\n'
    '    private boolean scp173WatchAwarded;\n')
replace_exact(scp131,
    '        if (scp173 != null) {\n'
    '            if (!wasWatchingScp173 && isFollowing()) {\n'
    '                dismissFollowersForScp173();\n'
    '            }\n'
    '            wasWatchingScp173 = true;\n',
    '        if (scp173 != null) {\n'
    '            if (!wasWatchingScp173) {\n'
    '                scp173WatchWitness = followOwner;\n'
    '                scp173WatchAwarded = false;\n'
    '                if (isFollowing()) dismissFollowersForScp173();\n'
    '            }\n'
    '            wasWatchingScp173 = true;\n')
replace_exact(scp131,
    '        if (wasWatchingScp173) {\n'
    '            wasWatchingScp173 = false;\n'
    '        }\n',
    '        if (wasWatchingScp173) {\n'
    '            wasWatchingScp173 = false;\n'
    '            scp173WatchWitness = null;\n'
    '            scp173WatchAwarded = false;\n'
    '        }\n')
replace_exact(scp131,
    '        } else {\n'
    '            getNavigation().stop();\n'
    '            setDeltaMovement(Vec3.ZERO);\n'
    '        }\n'
    '        lookHardAt(scp173);\n'
    '    }\n\n'
    '    private Vec3 watchSpotNear(Scp173Entity scp173) {\n',
    '        } else {\n'
    '            getNavigation().stop();\n'
    '            setDeltaMovement(Vec3.ZERO);\n'
    '            awardScp173WatchWitness();\n'
    '        }\n'
    '        lookHardAt(scp173);\n'
    '    }\n\n'
    '    private void awardScp173WatchWitness() {\n'
    '        if (scp173WatchAwarded\n'
    '                || !(level() instanceof ServerLevel serverLevel)) return;\n'
    '        ServerPlayer witness = scp173WatchWitness == null ? null\n'
    '                : serverLevel.getServer().getPlayerList()\n'
    '                .getPlayer(scp173WatchWitness);\n'
    '        if (witness == null || !witness.isAlive()\n'
    '                || witness.isCreative() || witness.isSpectator()\n'
    '                || witness.serverLevel() != serverLevel) {\n'
    '            double bestDistance = 24.0D * 24.0D;\n'
    '            witness = null;\n'
    '            for (ServerPlayer candidate : serverLevel.players()) {\n'
    '                if (!candidate.isAlive() || candidate.isCreative()\n'
    '                        || candidate.isSpectator()) continue;\n'
    '                double distance = distanceToSqr(candidate);\n'
    '                if (distance < bestDistance) {\n'
    '                    bestDistance = distance;\n'
    '                    witness = candidate;\n'
    '                }\n'
    '            }\n'
    '        }\n'
    '        if (witness != null) {\n'
    '            ScpAdvancementAwards.award(witness,\n'
    '                    ScpAdvancementAwards.EYES_ON_ME);\n'
    '        }\n'
    '        scp173WatchAwarded = true;\n'
    '    }\n\n'
    '    private Vec3 watchSpotNear(Scp173Entity scp173) {\n')

# SCP-012 + SCP-714: award only when 012 is actually open and able to influence
# the player, with 714 being the reason the influence is rejected.
scp012 = 'src/main/java/net/mcreator/scpadditions/scp012/Scp012InfluenceEvents.java'
replace_exact(scp012,
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n',
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n'
    'import net.mcreator.scpadditions.advancement.ScpAdvancementAwards;\n')
replace_exact(scp012,
    '        if (Scp714ProtectionAccess.isProtected(player)) {\n'
    '            clearInfluence(player);\n'
    '            return;\n'
    '        }\n'
    '        if (!Scp012Module.isOpen(level.getBlockState(nearby))) {\n'
    '            clearInfluence(player);\n'
    '            return;\n'
    '        }\n',
    '        if (!Scp012Module.isOpen(level.getBlockState(nearby))) {\n'
    '            clearInfluence(player);\n'
    '            return;\n'
    '        }\n'
    '        if (Scp714ProtectionAccess.isProtected(player)) {\n'
    '            ScpAdvancementAwards.award(player,\n'
    '                    ScpAdvancementAwards.WHAT);\n'
    '            clearInfluence(player);\n'
    '            return;\n'
    '        }\n')

# Complete server advancement catalog packet pair.
write('src/main/java/net/mcreator/scpadditions/network/AdvancementCatalogRequestPacket.java', '''package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request for the complete server advancement catalog. */
public final class AdvancementCatalogRequestPacket {
    public static void encode(AdvancementCatalogRequestPacket message,
            FriendlyByteBuf buffer) {
    }

    public static AdvancementCatalogRequestPacket decode(FriendlyByteBuf buffer) {
        return new AdvancementCatalogRequestPacket();
    }

    public static void handle(AdvancementCatalogRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) ScpEntityNetwork.sendAdvancementCatalog(sender);
        });
        context.setPacketHandled(true);
    }
}
''')

write('src/main/java/net/mcreator/scpadditions/network/AdvancementCatalogPacket.java', '''package net.mcreator.scpadditions.network;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.PauseMenuNativePanelsClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Complete server-side advancement catalog for the custom Achievements panel. */
public final class AdvancementCatalogPacket {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_ID_LENGTH = 256;
    private static final int MAX_COMPONENT_LENGTH = 16384;

    private final List<Entry> entries;

    public AdvancementCatalogPacket(List<Entry> entries) {
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static AdvancementCatalogPacket fromPlayer(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return new AdvancementCatalogPacket(List.of());
        }
        List<Entry> entries = new ArrayList<>();
        for (Advancement advancement : player.getServer().getAdvancements()
                .getAllAdvancements()) {
            DisplayInfo display = advancement.getDisplay();
            if (display == null) continue;
            Advancement root = advancement;
            while (root.getParent() != null) root = root.getParent();
            AdvancementProgress progress = player.getAdvancements()
                    .getOrStartProgress(advancement);
            entries.add(new Entry(advancement.getId().toString(),
                    root.getId().toString(), display.getTitle().copy(),
                    display.getDescription().copy(), display.getIcon().copy(),
                    display.getFrame(), display.isHidden(), progress.isDone()));
        }
        entries.sort(Comparator.comparing(Entry::rootId)
                .thenComparing(Entry::id));
        return new AdvancementCatalogPacket(entries);
    }

    public static void encode(AdvancementCatalogPacket message,
            FriendlyByteBuf buffer) {
        int size = Math.min(MAX_ENTRIES, message.entries.size());
        buffer.writeVarInt(size);
        for (int index = 0; index < size; index++) {
            Entry entry = message.entries.get(index);
            buffer.writeUtf(entry.id(), MAX_ID_LENGTH);
            buffer.writeUtf(entry.rootId(), MAX_ID_LENGTH);
            buffer.writeUtf(Component.Serializer.toJson(entry.title()),
                    MAX_COMPONENT_LENGTH);
            buffer.writeUtf(Component.Serializer.toJson(entry.description()),
                    MAX_COMPONENT_LENGTH);
            buffer.writeItem(entry.icon());
            buffer.writeVarInt(entry.frame().ordinal());
            buffer.writeBoolean(entry.hidden());
            buffer.writeBoolean(entry.done());
        }
    }

    public static AdvancementCatalogPacket decode(FriendlyByteBuf buffer) {
        int size = Mth.clamp(buffer.readVarInt(), 0, MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String id = buffer.readUtf(MAX_ID_LENGTH);
            String rootId = buffer.readUtf(MAX_ID_LENGTH);
            Component title = parseComponent(buffer.readUtf(MAX_COMPONENT_LENGTH));
            Component description = parseComponent(
                    buffer.readUtf(MAX_COMPONENT_LENGTH));
            ItemStack icon = buffer.readItem();
            int frameId = buffer.readVarInt();
            FrameType[] frames = FrameType.values();
            FrameType frame = frameId >= 0 && frameId < frames.length
                    ? frames[frameId] : FrameType.TASK;
            boolean hidden = buffer.readBoolean();
            boolean done = buffer.readBoolean();
            entries.add(new Entry(id, rootId, title, description, icon,
                    frame, hidden, done));
        }
        return new AdvancementCatalogPacket(entries);
    }

    public static void handle(AdvancementCatalogPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PauseMenuNativePanelsClient
                        .replaceAdvancementCatalog(message.entries)));
        context.setPacketHandled(true);
    }

    private static Component parseComponent(String json) {
        Component parsed = Component.Serializer.fromJson(json);
        return parsed == null ? Component.empty() : parsed;
    }

    public record Entry(String id, String rootId, Component title,
            Component description, ItemStack icon, FrameType frame,
            boolean hidden, boolean done) {
        public Entry {
            if (id == null) id = "";
            if (rootId == null) rootId = id;
            if (title == null) title = Component.empty();
            if (description == null) description = Component.empty();
            if (icon == null) icon = ItemStack.EMPTY;
            if (frame == null) frame = FrameType.TASK;
        }
    }
}
''')

network = 'src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java'
replace_exact(network,
    '        ScpAdditionsMod.addNetworkMessage(FacilityDiagnosticsResetPacket.class,\n'
    '                FacilityDiagnosticsResetPacket::encode,\n'
    '                FacilityDiagnosticsResetPacket::decode,\n'
    '                FacilityDiagnosticsResetPacket::handle);\n',
    '        ScpAdditionsMod.addNetworkMessage(FacilityDiagnosticsResetPacket.class,\n'
    '                FacilityDiagnosticsResetPacket::encode,\n'
    '                FacilityDiagnosticsResetPacket::decode,\n'
    '                FacilityDiagnosticsResetPacket::handle);\n'
    '        ScpAdditionsMod.addNetworkMessage(AdvancementCatalogRequestPacket.class,\n'
    '                AdvancementCatalogRequestPacket::encode,\n'
    '                AdvancementCatalogRequestPacket::decode,\n'
    '                AdvancementCatalogRequestPacket::handle);\n'
    '        ScpAdditionsMod.addNetworkMessage(AdvancementCatalogPacket.class,\n'
    '                AdvancementCatalogPacket::encode,\n'
    '                AdvancementCatalogPacket::decode,\n'
    '                AdvancementCatalogPacket::handle);\n')
replace_exact(network,
    '    public static void showScp131Notice(ServerPlayer player,\n',
    '    public static void sendAdvancementCatalog(ServerPlayer player) {\n'
    '        if (player == null) return;\n'
    '        ScpAdditionsMod.PACKET_HANDLER.send(\n'
    '                PacketDistributor.PLAYER.with(() -> player),\n'
    '                AdvancementCatalogPacket.fromPlayer(player));\n'
    '    }\n\n'
    '    public static void showScp131Notice(ServerPlayer player,\n')

mod = 'src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java'
replace_exact(mod,
    '    private static final String PROTOCOL_VERSION = "17";\n',
    '    private static final String PROTOCOL_VERSION = "18";\n')

# Custom Achievements browser.
panel = 'src/main/java/net/mcreator/scpadditions/client/PauseMenuNativePanelsClient.java'
replace_exact(panel,
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n',
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n'
    'import net.mcreator.scpadditions.network.AdvancementCatalogPacket;\n'
    'import net.mcreator.scpadditions.network.AdvancementCatalogRequestPacket;\n')
replace_exact(panel,
    'import java.util.List;\n',
    'import java.util.List;\nimport java.util.LinkedHashMap;\n')
replace_exact(panel,
    '        if (mode == Mode.ACHIEVEMENTS) {\n'
    '            rebuildAchievements(state);\n',
    '        if (mode == Mode.ACHIEVEMENTS) {\n'
    '            rebuildAchievements(state);\n'
    '            requestAdvancementCatalog();\n')
replace_exact(panel,
    '    private static State activeState(CustomPauseMenuScreen parent) {\n'
    '        State state = STATES.get(parent);\n'
    '        return state == null || !state.open || state.progress < 0.78F\n'
    '                || state.mode == null || state.layout == null ? null : state;\n'
    '    }\n\n',
    '    private static State activeState(CustomPauseMenuScreen parent) {\n'
    '        State state = STATES.get(parent);\n'
    '        return state == null || !state.open || state.progress < 0.78F\n'
    '                || state.mode == null || state.layout == null ? null : state;\n'
    '    }\n\n'
    '    private static void requestAdvancementCatalog() {\n'
    '        Minecraft minecraft = Minecraft.getInstance();\n'
    '        if (minecraft.getConnection() == null) return;\n'
    '        ScpAdditionsMod.PACKET_HANDLER.sendToServer(\n'
    '                new AdvancementCatalogRequestPacket());\n'
    '    }\n\n'
    '    public static void replaceAdvancementCatalog(\n'
    '            List<AdvancementCatalogPacket.Entry> entries) {\n'
    '        if (entries == null) return;\n'
    '        for (State state : STATES.values()) {\n'
    '            if (state != null && state.mode == Mode.ACHIEVEMENTS) {\n'
    '                rebuildAchievementsFromCatalog(state, entries);\n'
    '            }\n'
    '        }\n'
    '    }\n\n')
replace_exact(panel,
    '                boolean hidden = booleanValue(invokeNoArg(display, "isHidden"));\n'
    '                if (hidden && !done) continue;\n',
    '                boolean hidden = booleanValue(invokeNoArg(display, "isHidden"));\n')
replace_exact(panel,
    '                byRoot.get(root).add(new AdvancementRow(title, description,\n'
    '                        icon, done, frame, useModLogo));\n',
    '                byRoot.get(root).add(new AdvancementRow(advancementId,\n'
    '                        title, description, icon, done, frame,\n'
    '                        useModLogo, hidden));\n')
replace_exact(panel,
    '                rows.sort(Comparator.comparing(row ->\n'
    '                        row.title.getString().toLowerCase(Locale.ROOT)));\n',
    '                rows.sort(achievementRowComparator());\n', 1)
replace_exact(panel,
    '                state.achievementCategories.add(new AdvancementCategory(\n'
    '                        title, icon, useModLogo, completed, rows.size(),\n'
    '                        List.copyOf(rows)));\n',
    '                state.achievementCategories.add(new AdvancementCategory(\n'
    '                        idOf(root), title, icon, useModLogo, completed,\n'
    '                        rows.size(), List.copyOf(rows)));\n')

render_marker = '    private static void renderAchievements(GuiGraphics graphics, State state,\n'
catalog_method = '''    private static void rebuildAchievementsFromCatalog(State state,
            List<AdvancementCatalogPacket.Entry> entries) {
        AdvancementCategory previous = selectedCategory(state);
        String selectedRoot = previous == null ? null : previous.id;
        state.achievementCategories.clear();
        state.achievementScroll = 0;
        state.achievementCategoryScroll = 0;

        Map<String, List<AdvancementRow>> byRoot = new LinkedHashMap<>();
        Map<String, AdvancementCatalogPacket.Entry> rootEntries =
                new LinkedHashMap<>();
        for (AdvancementCatalogPacket.Entry entry : entries) {
            if (entry == null || entry.id().isBlank()
                    || entry.rootId().isBlank()) continue;
            byRoot.computeIfAbsent(entry.rootId(), ignored -> new ArrayList<>());
            if (entry.id().equals(entry.rootId())) {
                rootEntries.put(entry.rootId(), entry);
            }
            boolean useModLogo = (ScpAdditionsMod.MODID
                    + ":scp_additions_ach").equals(entry.id());
            byRoot.get(entry.rootId()).add(new AdvancementRow(entry.id(),
                    entry.title(), entry.description(), entry.icon(), entry.done(),
                    entry.frame(), useModLogo, entry.hidden()));
        }

        for (Map.Entry<String, List<AdvancementRow>> group : byRoot.entrySet()) {
            List<AdvancementRow> rows = group.getValue();
            if (rows.isEmpty()) continue;
            rows.sort(achievementRowComparator());
            String rootId = group.getKey();
            AdvancementCatalogPacket.Entry root = rootEntries.get(rootId);
            Component title = root == null
                    ? Component.literal(humanize(rootId)) : root.title();
            ItemStack icon = root == null ? ItemStack.EMPTY : root.icon();
            boolean useModLogo = (ScpAdditionsMod.MODID
                    + ":scp_additions_ach").equals(rootId);
            int completed = (int) rows.stream().filter(row -> row.done).count();
            state.achievementCategories.add(new AdvancementCategory(rootId,
                    title, icon, useModLogo, completed, rows.size(),
                    List.copyOf(rows)));
        }
        state.achievementCategories.sort(Comparator.comparing(category ->
                category.title.getString().toLowerCase(Locale.ROOT)));

        state.selectedAchievementCategory = 0;
        if (selectedRoot != null) {
            for (int index = 0; index < state.achievementCategories.size(); index++) {
                if (selectedRoot.equals(state.achievementCategories.get(index).id)) {
                    state.selectedAchievementCategory = index;
                    break;
                }
            }
        }
    }

    private static Comparator<AdvancementRow> achievementRowComparator() {
        return Comparator.comparing((AdvancementRow row) -> row.hidden)
                .thenComparing(row -> row.title.getString()
                        .toLowerCase(Locale.ROOT));
    }

'''
text = read(panel)
if text.count(render_marker) != 1:
    raise SystemExit('PauseMenuNativePanelsClient: render marker mismatch')
write(panel, text.replace(render_marker, catalog_method + render_marker, 1))

old_rows = '''            AdvancementRow entry = rows.get(index);
            int y = layout.contentY + row * (ACHIEVEMENT_ROW_HEIGHT + 5);
            boolean hovered = mouseX >= layout.contentX
                    && mouseX < layout.contentRight
                    && mouseY >= y && mouseY < y + ACHIEVEMENT_ROW_HEIGHT;
            graphics.fill(layout.contentX, y, layout.contentRight,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(hovered ? ROW_HOVER : ROW, alpha));
            graphics.fill(layout.contentX, y, layout.contentX + 3,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(entry.done ? ACCENT : BORDER, alpha));
            if (entry.useModLogo) {
                renderScpAdditionsLogo(graphics, layout.contentX + 9,
                        y + 13, alpha);
            } else if (!entry.icon.isEmpty()) {
                graphics.renderItem(entry.icon, layout.contentX + 9, y + 13);
            }
            int textX = layout.contentX + 34;
            String title = compactToWidth(font, entry.title.getString(),
                    layout.contentRight - textX - 86);
            graphics.drawString(font, ScpFonts.roboto(title), textX, y + 9,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : TEXT, alpha), false);
            String description = compactToWidth(font,
                    entry.description.getString(),
                    layout.contentRight - textX - 86);
            graphics.drawString(font, ScpFonts.roboto(description),
                    textX, y + 24, applyAlpha(MUTED, alpha), false);
            Component status = ScpFonts.titillium(entry.done ? "DONE" : "OPEN");
            graphics.drawString(font, status,
                    layout.contentRight - 10 - font.width(status), y + 9,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : MUTED, alpha), false);
            drawAchievementRarity(graphics, entry.frame,
                    layout.contentRight - 10, y + 26, alpha);
'''
new_rows = '''            AdvancementRow entry = rows.get(index);
            int y = layout.contentY + row * (ACHIEVEMENT_ROW_HEIGHT + 5);
            boolean hovered = mouseX >= layout.contentX
                    && mouseX < layout.contentRight
                    && mouseY >= y && mouseY < y + ACHIEVEMENT_ROW_HEIGHT;
            boolean concealed = entry.hidden && !entry.done;
            float rowAlpha = entry.done ? alpha
                    : alpha * (hovered ? 0.72F : 0.56F);
            graphics.fill(layout.contentX, y, layout.contentRight,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(hovered ? ROW_HOVER : ROW, rowAlpha));
            graphics.fill(layout.contentX, y, layout.contentX + 3,
                    y + ACHIEVEMENT_ROW_HEIGHT,
                    applyAlpha(entry.done ? ACCENT : BORDER, rowAlpha));
            int iconX = layout.contentX + 9;
            int iconY = y + 13;
            if (concealed) {
                drawHiddenAchievementIcon(graphics, iconX, iconY, alpha);
            } else if (entry.useModLogo) {
                renderScpAdditionsLogo(graphics, iconX, iconY, rowAlpha);
            } else if (!entry.icon.isEmpty()) {
                graphics.renderItem(entry.icon, iconX, iconY);
                if (!entry.done) {
                    graphics.fill(iconX, iconY, iconX + 16, iconY + 16,
                            applyAlpha(0xB00B0E12, alpha));
                }
            }
            int textX = layout.contentX + 34;
            String rawTitle = concealed ? "Hidden Achievement"
                    : entry.title.getString();
            String title = compactToWidth(font, rawTitle,
                    layout.contentRight - textX - 86);
            graphics.drawString(font, ScpFonts.roboto(title), textX, y + 9,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : MUTED,
                            entry.done ? alpha : alpha * 0.82F), false);
            if (!concealed) {
                String description = compactToWidth(font,
                        entry.description.getString(),
                        layout.contentRight - textX - 86);
                graphics.drawString(font, ScpFonts.roboto(description),
                        textX, y + 24,
                        applyAlpha(MUTED, entry.done ? alpha : alpha * 0.52F),
                        false);
            }
            Component status = ScpFonts.titillium(entry.done ? "DONE" : "OPEN");
            graphics.drawString(font, status,
                    layout.contentRight - 10 - font.width(status), y + 9,
                    applyAlpha(entry.done ? ACCENT_BRIGHT : MUTED, alpha), false);
            drawAchievementRarity(graphics, entry.frame,
                    layout.contentRight - 10, y + 26,
                    entry.done ? alpha : alpha * 0.82F);
'''
replace_exact(panel, old_rows, new_rows)

replace_exact(panel,
    '        if (frame == FrameType.CHALLENGE) {\n'
    '            filled = 5;\n'
    '            color = CHALLENGE;\n'
    '        } else if (frame == FrameType.GOAL) {\n'
    '            filled = 3;\n',
    '        if (frame == FrameType.CHALLENGE) {\n'
    '            filled = 3;\n'
    '            color = CHALLENGE;\n'
    '        } else if (frame == FrameType.GOAL) {\n'
    '            filled = 2;\n')
replace_exact(panel,
    '        int total = blockWidth * 5 + gap * 4;\n'
    '        int left = right - total;\n'
    '        for (int index = 0; index < 5; index++) {\n',
    '        int total = blockWidth * 3 + gap * 2;\n'
    '        int left = right - total;\n'
    '        for (int index = 0; index < 3; index++) {\n')

hidden_marker = '    private static void handleAchievementClick(State state,\n'
hidden_method = '''    private static void drawHiddenAchievementIcon(GuiGraphics graphics,
            int x, int y, float alpha) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + 16, y + 16,
                applyAlpha(PANEL_SOFT, alpha * 0.78F));
        graphics.fill(x, y, x + 16, y + 1, applyAlpha(BORDER, alpha));
        graphics.fill(x, y + 15, x + 16, y + 16, applyAlpha(BORDER, alpha));
        graphics.fill(x, y, x + 1, y + 16, applyAlpha(BORDER, alpha));
        graphics.fill(x + 15, y, x + 16, y + 16, applyAlpha(BORDER, alpha));
        Component question = ScpFonts.roboto("?");
        graphics.drawCenteredString(font, question, x + 8, y + 4,
                applyAlpha(MUTED, alpha));
    }

'''
text = read(panel)
if text.count(hidden_marker) != 1:
    raise SystemExit('PauseMenuNativePanelsClient: hidden icon marker mismatch')
write(panel, text.replace(hidden_marker, hidden_method + hidden_marker, 1))

replace_exact(panel,
    '    private record AdvancementCategory(Component title, ItemStack icon,\n'
    '            boolean useModLogo, int completed, int total,\n'
    '            List<AdvancementRow> rows) {\n'
    '    }\n\n'
    '    private record AdvancementRow(Component title, Component description,\n'
    '            ItemStack icon, boolean done, FrameType frame,\n'
    '            boolean useModLogo) {\n'
    '    }\n',
    '    private record AdvancementCategory(String id, Component title,\n'
    '            ItemStack icon, boolean useModLogo, int completed, int total,\n'
    '            List<AdvancementRow> rows) {\n'
    '    }\n\n'
    '    private record AdvancementRow(String id, Component title,\n'
    '            Component description, ItemStack icon, boolean done,\n'
    '            FrameType frame, boolean useModLogo, boolean hidden) {\n'
    '    }\n')

# Toast uses the same literal three-stage rarity meter.
toast = 'src/main/java/net/mcreator/scpadditions/client/CustomAdvancementToastClient.java'
replace_exact(toast,
    '        if (frame == FrameType.CHALLENGE) {\n'
    '            filled = 5;\n'
    '            color = CHALLENGE;\n'
    '            label = "CHALLENGE";\n'
    '        } else if (frame == FrameType.GOAL) {\n'
    '            filled = 3;\n',
    '        if (frame == FrameType.CHALLENGE) {\n'
    '            filled = 3;\n'
    '            color = CHALLENGE;\n'
    '            label = "CHALLENGE";\n'
    '        } else if (frame == FrameType.GOAL) {\n'
    '            filled = 2;\n')
replace_exact(toast,
    '        int total = barWidth * 5 + gap * 4;\n'
    '        int barLeft = right - total;\n'
    '        for (int index = 0; index < 5; index++) {\n',
    '        int total = barWidth * 3 + gap * 2;\n'
    '        int barLeft = right - total;\n'
    '        for (int index = 0; index < 3; index++) {\n')

# Advancement data.
advancements = {
    'from_the_trenches.json': {
        'icon': 'scp_additions:scp_106_spawn_egg',
        'title': 'advancements.from_the_trenches.title',
        'descr': 'advancements.from_the_trenches.descr',
        'frame': 'goal', 'hidden': False,
    },
    'eyes_on_me.json': {
        'icon': 'scp_additions:scp_131_a_spawn_egg',
        'title': 'advancements.eyes_on_me.title',
        'descr': 'advancements.eyes_on_me.descr',
        'frame': 'challenge', 'hidden': True,
    },
    'what.json': {
        'icon': 'scp_additions:scp_714',
        'title': 'advancements.what.title',
        'descr': 'advancements.what.descr',
        'frame': 'challenge', 'hidden': True,
    },
    'concrete_and_rebar.json': {
        'icon': 'scp_additions:scp_173_spawn_egg',
        'title': 'advancements.concrete_and_rebar.title',
        'descr': 'advancements.concrete_and_rebar.descr',
        'frame': 'goal', 'hidden': False,
    },
}
adv_dir = Path('src/main/resources/data/scp_additions/advancements')
for filename, data in advancements.items():
    doc = {
        'display': {
            'icon': {'item': data['icon']},
            'title': {'translate': data['title']},
            'description': {'translate': data['descr']},
            'frame': data['frame'],
            'show_toast': True,
            'announce_to_chat': True,
            'hidden': data['hidden'],
        },
        'criteria': {
            filename.removesuffix('.json'): {'trigger': 'minecraft:impossible'}
        },
        'rewards': {'experience': 10},
        'parent': 'scp_additions:scp_additions_ach',
    }
    write(adv_dir / filename, json.dumps(doc, indent=2) + '\n')

lang_path = Path('src/main/resources/assets/scp_additions/lang/en_us.json')
lang = json.loads(lang_path.read_text())
lang.update({
    'advancements.from_the_trenches.title': 'From the Trenches',
    'advancements.from_the_trenches.descr': 'Survive a hunt by SCP-106',
    'advancements.eyes_on_me.title': 'Eyes on me',
    'advancements.eyes_on_me.descr': 'Witness SCP-131 stop to observe SCP-173',
    'advancements.what.title': 'What?',
    'advancements.what.descr': "Resist SCP-012's influence with SCP-714",
    'advancements.concrete_and_rebar.title': 'Concrete and Rebar',
    'advancements.concrete_and_rebar.descr': 'Survive an encounter with SCP-173',
})
lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + '\n')

# Changelog additions.
changelog = Path('CHANGELOG.md')
text = changelog.read_text()
marker = '## SCP-106\n'
section = '''## Achievements

- Added **From the Trenches** for surviving an SCP-106 hunt;
- Added **Concrete and Rebar** for surviving an activated SCP-173 roamer encounter;
- Added the hidden **Eyes on me** achievement for witnessing SCP-131 stop to observe SCP-173;
- Added the hidden **What?** achievement for having SCP-714 prevent SCP-012 from taking hold;
- Reworked the custom Achievements panel to list the server's complete advancement catalog instead of inheriting vanilla visibility filtering, keeping every advancement category present even before its first completion;
- Unfinished advancements are now visually subdued, while unfinished hidden advancements are sorted to the end and shown only as **Hidden Achievement** placeholders with their rarity visible.

'''
if text.count(marker) != 1:
    raise SystemExit('CHANGELOG: SCP-106 marker mismatch')
changelog.write_text(text.replace(marker, section + marker, 1))

# One-shot scaffolding must not survive the functional commit.
Path('.github/workflows/_apply-advancement-browser-encounters.yml').unlink()
Path('.github/_temp_apply_advancement_browser.py').unlink()
