package net.mcreator.scpadditions.scp1576;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
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
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ScpAdditionsMod.MODID);

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
        EFFECTS.register(bus);
        SOUNDS.register(bus);
        Scp1576Network.register();
    }
}
