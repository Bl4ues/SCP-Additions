package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.registries.ForgeRegistries;

public final class CopycatPanelMaterial {
    public static final ModelProperty<ModelSnapshot> MODEL_PROPERTY =
            new ModelProperty<>();

    private CopycatPanelMaterial() {
    }

    public static ModelData modelData(BlockState front, BlockState back,
            BlockPos pos) {
        return ModelData.builder().with(MODEL_PROPERTY,
                new ModelSnapshot(safe(front), safe(back),
                        pos == null ? BlockPos.ZERO : pos.immutable())).build();
    }

    private static BlockState safe(BlockState state) {
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    public record ModelSnapshot(BlockState front, BlockState back,
            BlockPos pos) {
    }

    public static boolean valid(Level level, BlockPos pos, BlockState state,
            Block... forbidden) {
        if (state == null || state.isAir()
                || !state.getFluidState().isEmpty()
                || state.getRenderShape() != RenderShape.MODEL
                || !state.canOcclude()
                || !Block.isShapeFullBlock(state.getCollisionShape(level, pos))) {
            return false;
        }
        if (forbidden != null) {
            for (Block block : forbidden) {
                if (block != null && state.is(block)) return false;
            }
        }
        return true;
    }

    public static void writeBlock(CompoundTag tag, String key,
            BlockState state) {
        if (tag == null || key == null || state == null || state.isAir()) return;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null) tag.putString(key, id.toString());
    }

    public static BlockState readBlock(CompoundTag tag, String key) {
        if (tag == null || key == null || !tag.contains(key)) {
            return Blocks.AIR.defaultBlockState();
        }
        try {
            ResourceLocation id = new ResourceLocation(tag.getString(key));
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            return block == null ? Blocks.AIR.defaultBlockState()
                    : block.defaultBlockState();
        } catch (RuntimeException ignored) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    public static ItemStack item(BlockState state) {
        return state == null || state.isAir()
                ? ItemStack.EMPTY
                : new ItemStack(state.getBlock());
    }
}
