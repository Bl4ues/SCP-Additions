package com.bl4ues.scpclassifieddirective.facility.transform;

import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small codec wrapper so transformed construction data stays registry-safe. */
final class BlockStateCodec {
    private BlockStateCodec() {
    }

    static CompoundTag save(BlockState state) {
        CompoundTag wrapper = new CompoundTag();
        BlockState safe = state == null ? Blocks.AIR.defaultBlockState() : state;
        DataResult<Tag> encoded = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, safe);
        encoded.result().ifPresent(tag -> wrapper.put("Value", tag));
        return wrapper;
    }

    static BlockState load(CompoundTag wrapper) {
        if (wrapper == null || !wrapper.contains("Value")) {
            return Blocks.AIR.defaultBlockState();
        }
        Tag value = wrapper.get("Value");
        if (value == null) return Blocks.AIR.defaultBlockState();
        return BlockState.CODEC.parse(NbtOps.INSTANCE, value).result()
                .orElse(Blocks.AIR.defaultBlockState());
    }
}
