package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import com.bl4ues.scpclassifieddirective.block.Scp914PartBlock;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.item.Scp914BlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registry surface for the rebuilt SCP-914. */
public final class Scp914Module {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            ForgeRegistries.BLOCK_ENTITY_TYPES, ScpClassifiedDirectiveMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Block> SCP_914 = BLOCKS.register("scp_914", Scp914Block::new);
    public static final RegistryObject<Block> SCP_914_RESERVATION = BLOCKS.register(
            "scp_914_reservation",
            () -> new Scp914PartBlock(Scp914PartBlock.Kind.RESERVATION));
    public static final RegistryObject<Block> SCP_914_COLLISION = BLOCKS.register(
            "scp_914_collision",
            () -> new Scp914PartBlock(Scp914PartBlock.Kind.SOLID));
    public static final RegistryObject<Block> SCP_914_DOOR_COLLISION = BLOCKS.register(
            "scp_914_door_collision",
            () -> new Scp914PartBlock(Scp914PartBlock.Kind.DOOR));

    public static final RegistryObject<Item> SCP_914_ITEM = ITEMS.register("scp_914",
            () -> new Scp914BlockItem(SCP_914.get()));
    public static final RegistryObject<BlockEntityType<Scp914BlockEntity>> SCP_914_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("scp_914", () -> BlockEntityType.Builder.of(
                    Scp914BlockEntity::new, SCP_914.get()).build(null));

    // scp914refining already belongs to the generated global sound registry.
    // Re-registering the same id here creates a duplicate Forge registry owner
    // and crashes during registry freeze even though compileJava succeeds.
    public static final RegistryObject<SoundEvent> REFINING =
            ScpClassifiedDirectiveModSounds.SCP914REFINING;
    public static final RegistryObject<SoundEvent> WIND = ScpClassifiedDirectiveModSounds.SCP914KEY;
    public static final RegistryObject<SoundEvent> CLOSE = ScpClassifiedDirectiveModSounds.SCP914DOORCLOSE;
    public static final RegistryObject<SoundEvent> OPEN = ScpClassifiedDirectiveModSounds.SCP914DOOROPEN;
    public static final RegistryObject<SoundEvent> GEAR_1 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_2 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_3 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_4 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_5 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_6 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_7 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_8 = ScpClassifiedDirectiveModSounds.SCP914DIAL;
    public static final RegistryObject<SoundEvent> GEAR_9 = ScpClassifiedDirectiveModSounds.SCP914DIAL;

    private Scp914Module() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
    }

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, id)));
    }

    public static SoundEvent gearSound(int oneBasedIndex) {
        return switch (Math.max(1, Math.min(9, oneBasedIndex))) {
            case 1 -> GEAR_1.get();
            case 2 -> GEAR_2.get();
            case 3 -> GEAR_3.get();
            case 4 -> GEAR_4.get();
            case 5 -> GEAR_5.get();
            case 6 -> GEAR_6.get();
            case 7 -> GEAR_7.get();
            case 8 -> GEAR_8.get();
            default -> GEAR_9.get();
        };
    }
}
