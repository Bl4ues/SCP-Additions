package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpadditions.compat.LazyOptional;
import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Persistent player inventory attachment replacing the removed Forge capability API. */
public final class ScpInventoryCapability {
    public static final DeferredRegister<AttachmentType<?>> REGISTRY =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                    ScpInventoryMod.MODID);

    public static final Supplier<AttachmentType<IScpInventory>> INSTANCE =
            REGISTRY.register("scp_inventory", () -> AttachmentType
                    .builder(() -> (IScpInventory) new ScpInventory())
                    .serialize(new IAttachmentSerializer<CompoundTag, IScpInventory>() {
                        @Override
                        public IScpInventory read(IAttachmentHolder holder,
                                CompoundTag tag, HolderLookup.Provider provider) {
                            ScpInventory inventory = new ScpInventory();
                            inventory.deserializeNBT(tag, provider);
                            return inventory;
                        }

                        @Override
                        public CompoundTag write(IScpInventory inventory,
                                HolderLookup.Provider provider) {
                            return inventory.serializeNBT(provider);
                        }
                    })
                    .copyOnDeath()
                    .build());

    private ScpInventoryCapability() {
    }

    public static LazyOptional<IScpInventory> get(Player player) {
        return player == null
                ? LazyOptional.empty()
                : LazyOptional.of(() -> player.getData(INSTANCE));
    }
}
