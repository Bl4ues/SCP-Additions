package net.mcreator.scpadditions.fabric;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
public final class FabricPersistentData {
    private static final AttachmentType<CompoundTag> DATA=AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("scp_additions","persistent_entity_data"),
        builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath());
    private FabricPersistentData(){}
    public static CompoundTag get(Entity entity){return entity.getAttachedOrCreate(DATA);}
}
