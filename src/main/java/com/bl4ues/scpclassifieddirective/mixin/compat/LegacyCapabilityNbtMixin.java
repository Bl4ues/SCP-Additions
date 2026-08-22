package com.bl4ues.scpclassifieddirective.mixin.compat;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

@Mixin(value = CapabilityDispatcher.class, remap = false)
public abstract class LegacyCapabilityNbtMixin {
    private static final List<String> LEGACY_NAMESPACES = List.of(
            "scp_additions", "scp_unity_extra_blocks", "scp_ublocks", "scpinventory");

    @Inject(method = "deserializeNBT", at = @At("HEAD"))
    private void scpClassifiedDirective$migrateLegacyCapabilityKeys(CompoundTag nbt, CallbackInfo ci) {
        for (String legacyNamespace : LEGACY_NAMESPACES) {
            String prefix = legacyNamespace + ":";
            for (String key : List.copyOf(nbt.getAllKeys())) {
                if (!key.startsWith(prefix)) continue;
                String migrated = ScpClassifiedDirectiveMod.MODID + key.substring(legacyNamespace.length());
                if (nbt.contains(migrated)) continue;
                Tag value = nbt.get(key);
                if (value != null) nbt.put(migrated, value.copy());
            }
        }
    }
}
