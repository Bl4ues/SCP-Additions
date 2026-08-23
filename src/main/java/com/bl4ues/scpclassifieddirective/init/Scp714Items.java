package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp714PlacedBlock;
import com.bl4ues.scpclassifieddirective.item.Scp714Item;

/** Registry isolated for SCP-714. */
public final class Scp714Items {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> SCP_714_PLACED =
            BLOCKS.register("scp_714_placed", Scp714PlacedBlock::new);
    public static final RegistryObject<Item> SCP_714 =
            REGISTRY.register("scp_714", Scp714Item::new);

    private Scp714Items() {
    }
}
