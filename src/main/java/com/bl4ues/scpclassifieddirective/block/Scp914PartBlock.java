package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible structural cell used by the rebuilt, oversized SCP-914. */
public final class Scp914PartBlock extends Block {
    public enum Kind {
        RESERVATION,
        SOLID,
        DOOR
    }

    private final Kind kind;

    public Scp914PartBlock(Kind kind) {
        super(properties(kind));
        this.kind = kind;
    }

    private static BlockBehaviour.Properties properties(Kind kind) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .noLootTable();
        if (kind == Kind.RESERVATION) properties = properties.noCollission();
        return properties;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        if (kind == Kind.RESERVATION) return Shapes.empty();
        if (kind == Kind.SOLID) return Shapes.block();
        return doorClosed(level, pos) ? Shapes.block() : Shapes.empty();
    }

    private static boolean doorClosed(BlockGetter level, BlockPos partPos) {
        for (BlockPos candidate : BlockPos.betweenClosed(
                partPos.offset(-9, -3, -9), partPos.offset(9, 3, 9))) {
            if (!(level.getBlockEntity(candidate) instanceof Scp914BlockEntity machine)) {
                continue;
            }
            if (!Scp914Structure.isDoorCell(candidate,
                    Scp914Structure.facing(machine.getBlockState()), partPos)) {
                continue;
            }
            long elapsed = machine.refiningElapsedTicks();
            return machine.isRefining()
                    && elapsed >= Scp914BlockEntity.DOOR_CLOSE_SOUND_TICK
                    && elapsed < Scp914BlockEntity.PROCESS_AND_OPEN_TICK;
        }
        return false;
    }
}
