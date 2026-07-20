package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpadditions.compat.LazyOptional;
import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import java.util.function.Supplier;

/** Persistent player inventory attachment replacing the removed Forge capability API. */
public final class ScpInventoryCapability {
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<IScpInventory> INSTANCE =
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_inventory"),
                    builder -> builder.initializer(ScpInventory::new)
                            .persistent(FabricScpInventoryCodec.CODEC)
                            .copyOnDeath());

    private ScpInventoryCapability() {
    }

    public static LazyOptional<IScpInventory> get(Player player) {
        return player == null
                ? LazyOptional.empty()
                : LazyOptional.of(() -> ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).getAttachedOrCreate(INSTANCE));
    }
}
