package net.mcreator.scpadditions.scp1576;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp1576Network;

/** Registry surface for SCP-1576. */
public final class Scp1576Module {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpAdditionsMod.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Block> PLACED_BLOCK = BLOCKS.register(
            "scp_1576_placed", Scp1576PlacedBlock::new);
    public static final RegistryObject<BlockEntityType<Scp1576PlacedBlockEntity>>
            PLACED_BLOCK_ENTITY = BLOCK_ENTITIES.register("scp_1576_placed",
            () -> BlockEntityType.Builder.of(Scp1576PlacedBlockEntity::new,
                    PLACED_BLOCK.get()).build(null));
    public static final RegistryObject<Item> SCP_1576 = ITEMS.register(
            "scp_1576", Scp1576Item::new);
    public static final RegistryObject<MobEffect> SCP_1576_EFFECT = EFFECTS.register(
            "scp_1576", Scp1576Effect::new);
    public static final RegistryObject<SoundEvent> WIND = SOUNDS.register(
            "scp1576_wind", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ScpAdditionsMod.MODID,
                            "scp1576_wind")));
    public static final RegistryObject<SoundEvent> SPEAK = SOUNDS.register(
            "scp1576_speak", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ScpAdditionsMod.MODID,
                            "scp1576_speak")));

    private Scp1576Module() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        EFFECTS.register(bus);
        SOUNDS.register(bus);
        Scp1576Network.register();
    }
}
