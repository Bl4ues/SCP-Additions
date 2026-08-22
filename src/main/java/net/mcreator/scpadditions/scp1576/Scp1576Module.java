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

/** Registry surface for SCP-1576 and its isolated sound catalog. */
public final class Scp1576Module {
    public static final String SOUND_NAMESPACE = "scp_additions_1576";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, ScpAdditionsMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, SOUND_NAMESPACE);

    public static final RegistryObject<Item> SCP_1576 = ITEMS.register(
            "scp_1576", Scp1576Item::new);
    public static final RegistryObject<MobEffect> SCP_1576_EFFECT = EFFECTS.register(
            "scp_1576", Scp1576Effect::new);
    public static final RegistryObject<SoundEvent> WIND = SOUNDS.register(
            "wind", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(SOUND_NAMESPACE, "wind")));
    public static final RegistryObject<SoundEvent> SPEAK = SOUNDS.register(
            "speak", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(SOUND_NAMESPACE, "speak")));

    private Scp1576Module() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        EFFECTS.register(bus);
        SOUNDS.register(bus);
    }
}
