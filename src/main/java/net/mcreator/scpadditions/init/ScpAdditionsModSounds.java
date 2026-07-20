
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ScpAdditionsMod.MODID);
	public static final Supplier<SoundEvent> TESLAACTIVATE = REGISTRY.register("teslaactivate", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "teslaactivate")));
	public static final Supplier<SoundEvent> OVERCHARGE = REGISTRY.register("overcharge", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "overcharge")));
	public static final Supplier<SoundEvent> TESLAREADY = REGISTRY.register("teslaready", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "teslaready")));
	public static final Supplier<SoundEvent> TESLARECHARGE = REGISTRY.register("teslarecharge", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "teslarecharge")));
	public static final Supplier<SoundEvent> CLICK = REGISTRY.register("click", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "click")));
	public static final Supplier<SoundEvent> CLICK_1 = REGISTRY.register("click_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "click_1")));
	public static final Supplier<SoundEvent> CLICK_2 = REGISTRY.register("click_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "click_2")));
	public static final Supplier<SoundEvent> SELECT = REGISTRY.register("select", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "select")));
	public static final Supplier<SoundEvent> POPUP = REGISTRY.register("popup", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "popup")));
	public static final Supplier<SoundEvent> TURNINGON = REGISTRY.register("turningon", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "turningon")));
	public static final Supplier<SoundEvent> TURNINGOFF = REGISTRY.register("turningoff", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "turningoff")));
	public static final Supplier<SoundEvent> OVERRIDEON = REGISTRY.register("overrideon", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "overrideon")));
	public static final Supplier<SoundEvent> TERMINALLOOP = REGISTRY.register("terminalloop", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "terminalloop")));
	public static final Supplier<SoundEvent> TERMINALON = REGISTRY.register("terminalon", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "terminalon")));
	public static final Supplier<SoundEvent> TERMINALOFF = REGISTRY.register("terminaloff", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "terminaloff")));
	public static final Supplier<SoundEvent> SCP079_1 = REGISTRY.register("scp079_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp079_1")));
	public static final Supplier<SoundEvent> CANDYEAT = REGISTRY.register("candyeat", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "candyeat")));
	public static final Supplier<SoundEvent> CANDY = REGISTRY.register("candy", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "candy")));
	public static final Supplier<SoundEvent> SCP330DEATH = REGISTRY.register("scp330death", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp330death")));
	public static final Supplier<SoundEvent> SCP1176 = REGISTRY.register("scp1176", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp1176")));
	public static final Supplier<SoundEvent> SCP902 = REGISTRY.register("scp902", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp902")));
	public static final Supplier<SoundEvent> SCP902CLOSING = REGISTRY.register("scp902closing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp902closing")));
	public static final Supplier<SoundEvent> SCP902OPENING = REGISTRY.register("scp902opening", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp902opening")));
	public static final Supplier<SoundEvent> SCP079_2 = REGISTRY.register("scp079_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp079_2")));
	public static final Supplier<SoundEvent> BUTTON = REGISTRY.register("button", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "button")));
	public static final Supplier<SoundEvent> SCP914DOORCLOSE = REGISTRY.register("scp914doorclose", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914doorclose")));
	public static final Supplier<SoundEvent> SCP914DOOROPEN = REGISTRY.register("scp914dooropen", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914dooropen")));
	public static final Supplier<SoundEvent> SCP914KEY = REGISTRY.register("scp914key", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914key")));
	public static final Supplier<SoundEvent> SCP914REFINING = REGISTRY.register("scp914refining", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914refining")));
	public static final Supplier<SoundEvent> SCP914DIAL = REGISTRY.register("scp914dial", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914dial")));
	public static final Supplier<SoundEvent> SCP914INSIDE = REGISTRY.register("scp914inside", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914inside")));
	public static final Supplier<SoundEvent> SCP914DEATH = REGISTRY.register("scp914death", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp914death")));
	public static final Supplier<SoundEvent> SPRAY = REGISTRY.register("spray", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "spray")));
	public static final Supplier<SoundEvent> DECONTAMINATION = REGISTRY.register("decontamination", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "decontamination")));
	public static final Supplier<SoundEvent> DOOROPEN = REGISTRY.register("dooropen", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "dooropen")));
	public static final Supplier<SoundEvent> DOORCLOSING = REGISTRY.register("doorclosing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "doorclosing")));
	public static final Supplier<SoundEvent> ACCESSGRANTED = REGISTRY.register("accessgranted", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "accessgranted")));
	public static final Supplier<SoundEvent> ACCESSDENIED = REGISTRY.register("accessdenied", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "accessdenied")));
	public static final Supplier<SoundEvent> SCP294ENTER = REGISTRY.register("scp294enter", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294enter")));
	public static final Supplier<SoundEvent> SCP294POURING = REGISTRY.register("scp294pouring", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294pouring")));
	public static final Supplier<SoundEvent> SCP294EMPTYCUP = REGISTRY.register("scp294emptycup", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294emptycup")));
	public static final Supplier<SoundEvent> SCP294OUTOFRANGE = REGISTRY.register("scp294outofrange", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294outofrange")));
	public static final Supplier<SoundEvent> SCP294ON = REGISTRY.register("scp294on", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294on")));
	public static final Supplier<SoundEvent> SCP294OFF = REGISTRY.register("scp294off", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294off")));
	public static final Supplier<SoundEvent> SCP294COINSLOT = REGISTRY.register("scp294coinslot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp294coinslot")));
	public static final Supplier<SoundEvent> HEARTBEAT = REGISTRY.register("heartbeat", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "heartbeat")));
	public static final Supplier<SoundEvent> NUCLEAR = REGISTRY.register("nuclear", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "nuclear")));
	public static final Supplier<SoundEvent> GRAVITONS = REGISTRY.register("gravitons", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "gravitons")));
	public static final Supplier<SoundEvent> STOMACH = REGISTRY.register("stomach", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "stomach")));
	public static final Supplier<SoundEvent> HAZMAT_EQUIP = REGISTRY.register("hazmat_equip", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "hazmat_equip")));
	public static final Supplier<SoundEvent> HAZMAT_REMOVE = REGISTRY.register("hazmat_remove", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "hazmat_remove")));
	public static final Supplier<SoundEvent> HAZMAT_BREATHING = REGISTRY.register("hazmat_breathing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "hazmat_breathing")));
	public static final Supplier<SoundEvent> SCP_714_MUSIC = REGISTRY.register("scp_714", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_714")));
	public static final Supplier<SoundEvent> SCP012_TRANCE = REGISTRY.register("scp012_trance", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_trance")));
	public static final Supplier<SoundEvent> SCP012_DAMAGE = REGISTRY.register("scp012_damage", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_damage")));
	public static final Supplier<SoundEvent> SCP012_OPEN = REGISTRY.register("scp012_open", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_open")));
	public static final Supplier<SoundEvent> SCP012_CLOSE = REGISTRY.register("scp012_close", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_close")));
	public static final Supplier<SoundEvent> SCP012_BLEED_1 = REGISTRY.register("scp012_bleed_1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_bleed_1")));
	public static final Supplier<SoundEvent> SCP012_BLEED_2 = REGISTRY.register("scp012_bleed_2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_bleed_2")));
	public static final Supplier<SoundEvent> SCP012_BLEED_3 = REGISTRY.register("scp012_bleed_3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_bleed_3")));
	public static final Supplier<SoundEvent> SCP012_ON_MOUNT_GOLGOTHA = REGISTRY.register("scp012_on_mount_golgotha", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("scp_additions", "scp012_on_mount_golgotha")));
}
