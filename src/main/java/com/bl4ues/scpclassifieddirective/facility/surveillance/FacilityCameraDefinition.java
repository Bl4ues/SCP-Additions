package com.bl4ues.scpclassifieddirective.facility.surveillance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Persistent, block-agnostic definition of a facility surveillance viewpoint. */
public record FacilityCameraDefinition(UUID id, ResourceLocation dimension,
        BlockPos anchorPos, Vec3 eyePosition, String name,
        float baseYaw, float basePitch, float yawLimit,
        float minPitch, float maxPitch, float maxZoom) {
    public FacilityCameraDefinition {
        id = id == null ? UUID.randomUUID() : id;
        dimension = dimension == null
                ? new ResourceLocation("minecraft", "overworld") : dimension;
        anchorPos = anchorPos == null ? BlockPos.ZERO : anchorPos.immutable();
        eyePosition = eyePosition == null ? Vec3.atCenterOf(anchorPos)
                : eyePosition;
        name = sanitize(name);
        yawLimit = Mth.clamp(Math.abs(yawLimit), 0.0F, 180.0F);
        minPitch = Mth.clamp(minPitch, -89.0F, 89.0F);
        maxPitch = Mth.clamp(maxPitch, -89.0F, 89.0F);
        if (minPitch > maxPitch) {
            float swap = minPitch;
            minPitch = maxPitch;
            maxPitch = swap;
        }
        basePitch = Mth.clamp(basePitch, minPitch, maxPitch);
        maxZoom = Mth.clamp(maxZoom, 1.0F, 5.0F);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Anchor", anchorPos.asLong());
        tag.putDouble("EyeX", eyePosition.x);
        tag.putDouble("EyeY", eyePosition.y);
        tag.putDouble("EyeZ", eyePosition.z);
        tag.putString("Name", name);
        tag.putFloat("BaseYaw", baseYaw);
        tag.putFloat("BasePitch", basePitch);
        tag.putFloat("YawLimit", yawLimit);
        tag.putFloat("MinPitch", minPitch);
        tag.putFloat("MaxPitch", maxPitch);
        tag.putFloat("MaxZoom", maxZoom);
        return tag;
    }

    public static FacilityCameraDefinition load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString("Dimension"));
        if (dimension == null) return null;
        BlockPos anchor = BlockPos.of(tag.getLong("Anchor"));
        return new FacilityCameraDefinition(tag.getUUID("Id"), dimension,
                anchor, new Vec3(tag.getDouble("EyeX"), tag.getDouble("EyeY"),
                tag.getDouble("EyeZ")), tag.getString("Name"),
                tag.getFloat("BaseYaw"), tag.getFloat("BasePitch"),
                tag.contains("YawLimit") ? tag.getFloat("YawLimit") : 75.0F,
                tag.contains("MinPitch") ? tag.getFloat("MinPitch") : -55.0F,
                tag.contains("MaxPitch") ? tag.getFloat("MaxPitch") : 55.0F,
                tag.contains("MaxZoom") ? tag.getFloat("MaxZoom") : 2.25F);
    }

    private static String sanitize(String value) {
        String clean = value == null ? "Camera" : value.strip()
                .replace('\n', ' ').replace('\r', ' ');
        if (clean.isBlank()) clean = "Camera";
        return clean.length() > 64 ? clean.substring(0, 64) : clean;
    }
}
