package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.Scp714Item;

/** Registry isolated for the SCP-714 feature branch. */
public final class Scp714Items {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.ITEM,
                    ScpAdditionsMod.MODID);

    public static final Supplier<Item> SCP_714 =
            REGISTRY.register("scp_714", Scp714Item::new);

    private Scp714Items() {
    }
}
