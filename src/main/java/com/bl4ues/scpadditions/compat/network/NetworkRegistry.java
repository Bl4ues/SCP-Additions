package com.bl4ues.scpadditions.compat.network;

import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

/** Forge 1.20.1 channel factory compatibility shim. */
public final class NetworkRegistry {
    private NetworkRegistry() {
    }

    public static SimpleChannel newSimpleChannel(
            ResourceLocation channelId,
            Supplier<String> version,
            Predicate<String> clientAcceptedVersions,
            Predicate<String> serverAcceptedVersions) {
        return new SimpleChannel(channelId, version.get());
    }
}
