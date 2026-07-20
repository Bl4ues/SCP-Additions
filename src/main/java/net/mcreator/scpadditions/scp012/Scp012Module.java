package net.mcreator.scpadditions.scp012;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Registry and state transitions for SCP-012's animated containment box. */
public final class Scp012Module {
    private static final int CLOSING_LEAD_IN_TICKS = 30;
    private static final Set<PendingClose> PENDING_CLOSES = new HashSet<>();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            BuiltInRegistries.BLOCK, ScpAdditionsMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            BuiltInRegistries.ITEM, ScpAdditionsMod.MODID);

    public static final Supplier<Block> CLOSED =
            stage("scp_012", Scp012Stage.CLOSED);
    public static final Supplier<Block> OPENING_1 =
            stage("scp_012_opening_1", Scp012Stage.OPENING_1);
    public static final Supplier<Block> OPENING_2 =
            stage("scp_012_opening_2", Scp012Stage.OPENING_2);
    public static final Supplier<Block> OPENING_3 =
            stage("scp_012_opening_3", Scp012Stage.OPENING_3);
    public static final Supplier<Block> OPENING_4 =
            stage("scp_012_opening_4", Scp012Stage.OPENING_4);
    public static final Supplier<Block> OPEN =
            stage("scp_012_open", Scp012Stage.OPEN);
    public static final Supplier<Block> CLOSING_4 =
            stage("scp_012_closing_4", Scp012Stage.CLOSING_4);
    public static final Supplier<Block> CLOSING_3 =
            stage("scp_012_closing_3", Scp012Stage.CLOSING_3);
    public static final Supplier<Block> CLOSING_2 =
            stage("scp_012_closing_2", Scp012Stage.CLOSING_2);
    public static final Supplier<Block> CLOSING_1 =
            stage("scp_012_closing_1", Scp012Stage.CLOSING_1);

    public static final Supplier<Item> SCP_012_ITEM = ITEMS.register("scp_012",
            () -> new BlockItem(CLOSED.get(), new Item.Properties().stacksTo(1)) {
                @Override
                public Component getName(ItemStack stack) {
                    return Component.literal("SCP-012");
                }

                @Override
                public void appendHoverText(ItemStack stack, Level level,
                                            List<Component> tooltip,
                                            TooltipFlag flag) {
                    tooltip.add(Component.literal("A Bad Composition"));
                }
            });

    private Scp012Module() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static Supplier<Block> stage(String id, Scp012Stage stage) {
        return BLOCKS.register(id, () -> new Scp012Block(stage));
    }

    public static boolean isScp012(BlockState state) {
        return stageOf(state) != null;
    }

    public static boolean isOpen(BlockState state) {
        return stageOf(state) == Scp012Stage.OPEN;
    }

    public static Scp012Stage stageOf(BlockState state) {
        return state.getBlock() instanceof Scp012Block block ? block.stage() : null;
    }

    public static boolean open(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (stageOf(current) != Scp012Stage.CLOSED) return false;
        replace(level, pos, current, OPENING_1.get());
        level.playSound(null, pos, ScpAdditionsModSounds.SCP012_OPEN.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static boolean close(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (stageOf(current) != Scp012Stage.OPEN) return false;

        BlockPos immutablePos = pos.immutable();
        PendingClose pending = new PendingClose(level.dimension(), immutablePos);
        if (!PENDING_CLOSES.add(pending)) return false;

        // The authored cue is a one-and-a-half-second lead-in. Play it immediately,
        // keep the box visibly open for thirty ticks, and only then begin the
        // three-second closing animation. The pending key prevents duplicate
        // sounds/tasks.
        level.playSound(null, immutablePos, ScpAdditionsModSounds.SCP012_CLOSE.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        ScpAdditionsMod.queueServerWork(CLOSING_LEAD_IN_TICKS, () -> {
            PENDING_CLOSES.remove(pending);
            if (!level.isLoaded(immutablePos)) return;

            BlockState delayedState = level.getBlockState(immutablePos);
            if (stageOf(delayedState) == Scp012Stage.OPEN) {
                replace(level, immutablePos, delayedState, CLOSING_4.get());
            }
        });
        return true;
    }

    public static boolean toggle(ServerLevel level, BlockPos pos) {
        Scp012Stage stage = stageOf(level.getBlockState(pos));
        if (stage == null) return false;
        return stage == Scp012Stage.CLOSED
                ? open(level, pos) : stage == Scp012Stage.OPEN
                && close(level, pos);
    }

    static void advanceAnimation(ServerLevel level, BlockPos pos,
                                 BlockState state, Scp012Stage stage) {
        Block next = switch (stage) {
            case OPENING_1 -> OPENING_2.get();
            case OPENING_2 -> OPENING_3.get();
            case OPENING_3 -> OPENING_4.get();
            case OPENING_4 -> OPEN.get();
            case CLOSING_4 -> CLOSING_3.get();
            case CLOSING_3 -> CLOSING_2.get();
            case CLOSING_2 -> CLOSING_1.get();
            case CLOSING_1 -> CLOSED.get();
            default -> null;
        };
        if (next != null) replace(level, pos, state, next);
    }

    private static void replace(ServerLevel level, BlockPos pos,
                                BlockState oldState, Block next) {
        Direction facing = oldState.hasProperty(HorizontalDirectionalBlock.FACING)
                ? oldState.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        level.setBlock(pos, next.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
    }

    public static Vec3 attractionPoint(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        return Vec3.atCenterOf(pos).add(facing.getStepX() * 0.85D,
                -0.20D, facing.getStepZ() * 0.85D);
    }

    public static BlockPos findNearest(ServerLevel level, Vec3 origin,
                                       int radius, boolean requireOpen) {
        BlockPos center = BlockPos.containing(origin);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockPos pos = mutable.immutable();
            BlockState state = level.getBlockState(pos);
            if (!isScp012(state) || requireOpen && !isOpen(state)) continue;
            double distance = Vec3.atCenterOf(pos).distanceToSqr(origin);
            if (distance <= radius * radius && distance < bestDistance) {
                best = pos;
                bestDistance = distance;
            }
        }
        return best;
    }

    private record PendingClose(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
