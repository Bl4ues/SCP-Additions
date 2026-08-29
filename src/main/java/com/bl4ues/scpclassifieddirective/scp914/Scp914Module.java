package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import com.bl4ues.scpclassifieddirective.block.Scp914PartBlock;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
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

    public static final RegistryObject<SoundEvent> REFINING = sound("scp914refining");
    public static final RegistryObject<SoundEvent> WIND = sound("scp914wind");
    public static final RegistryObject<SoundEvent> CLOSE = sound("scp914close");
    public static final RegistryObject<SoundEvent> OPEN = sound("scp914open");
    public static final RegistryObject<SoundEvent> GEAR_1 = sound("scp914gear_1");
    public static final RegistryObject<SoundEvent> GEAR_2 = sound("scp914gear_2");
    public static final RegistryObject<SoundEvent> GEAR_3 = sound("scp914gear_3");
    public static final RegistryObject<SoundEvent> GEAR_4 = sound("scp914gear_4");
    public static final RegistryObject<SoundEvent> GEAR_5 = sound("scp914gear_5");
    public static final RegistryObject<SoundEvent> GEAR_6 = sound("scp914gear_6");
    public static final RegistryObject<SoundEvent> GEAR_7 = sound("scp914gear_7");
    public static final RegistryObject<SoundEvent> GEAR_8 = sound("scp914gear_8");
    public static final RegistryObject<SoundEvent> GEAR_9 = sound("scp914gear_9");

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
