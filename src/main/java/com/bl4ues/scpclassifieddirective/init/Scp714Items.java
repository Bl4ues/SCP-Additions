package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.Scp714Item;

/** Registry isolated for the SCP-714 feature branch. */
public final class Scp714Items {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Item> SCP_714 =
            REGISTRY.register("scp_714", Scp714Item::new);

    private Scp714Items() {
    }
}
