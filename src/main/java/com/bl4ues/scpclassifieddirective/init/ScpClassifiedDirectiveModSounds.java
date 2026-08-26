
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

public class ScpClassifiedDirectiveModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpClassifiedDirectiveMod.MODID);
	public static final RegistryObject<SoundEvent> TESLAACTIVATE = REGISTRY.register("teslaactivate", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "teslaactivate")));
	public static final RegistryObject<SoundEvent> OVERCHARGE = REGISTRY.register("overcharge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "overcharge")));
	public static final RegistryObject<SoundEvent> TESLAREADY = REGISTRY.register("teslaready", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "teslaready")));
	public static final RegistryObject<SoundEvent> TESLARECHARGE = REGISTRY.register("teslarecharge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "teslarecharge")));
	public static final RegistryObject<SoundEvent> TESLA_ALARM = REGISTRY.register("tesla_alarm", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "tesla_alarm")));
	public static final RegistryObject<SoundEvent> TESLA_DISCHARGE = REGISTRY.register("tesla_discharge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "tesla_discharge")));
	public static final RegistryObject<SoundEvent> TESLA_OVERRIDE_DISCHARGE = REGISTRY.register("tesla_override_discharge", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "tesla_override_discharge")));
	public static final RegistryObject<SoundEvent> TESLA_LOOP = REGISTRY.register("tesla_loop", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "tesla_loop")));
	public static final RegistryObject<SoundEvent> CLICK = REGISTRY.register("click", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "click")));
	public static final RegistryObject<SoundEvent> CLICK_1 = REGISTRY.register("click_1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "click_1")));
	public static final RegistryObject<SoundEvent> CLICK_2 = REGISTRY.register("click_2", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "click_2")));
	public static final RegistryObject<SoundEvent> SELECT = REGISTRY.register("select", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "select")));
	public static final RegistryObject<SoundEvent> POPUP = REGISTRY.register("popup", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "popup")));
	public static final RegistryObject<SoundEvent> TURNINGON = REGISTRY.register("turningon", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "turningon")));
	public static final RegistryObject<SoundEvent> TURNINGOFF = REGISTRY.register("turningoff", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "turningoff")));
	public static final RegistryObject<SoundEvent> OVERRIDEON = REGISTRY.register("overrideon", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "overrideon")));
	public static final RegistryObject<SoundEvent> TERMINALLOOP = REGISTRY.register("terminalloop", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "terminalloop")));
	public static final RegistryObject<SoundEvent> AUXGEN = REGISTRY.register("auxgen", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "auxgen")));
	public static final RegistryObject<SoundEvent> TERMINALON = REGISTRY.register("terminalon", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "terminalon")));
	public static final RegistryObject<SoundEvent> TERMINALOFF = REGISTRY.register("terminaloff", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "terminaloff")));
	public static final RegistryObject<SoundEvent> SCP079_1 = REGISTRY.register("scp079_1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp079_1")));
	public static final RegistryObject<SoundEvent> SCP079HACK = REGISTRY.register("scp079_hack", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp079_hack")));
	public static final RegistryObject<SoundEvent> CANDYEAT = REGISTRY.register("candyeat", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "candyeat")));
	public static final RegistryObject<SoundEvent> CANDY = REGISTRY.register("candy", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "candy")));
	public static final RegistryObject<SoundEvent> SCP330DEATH = REGISTRY.register("scp330death", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp330death")));
	public static final RegistryObject<SoundEvent> SCP1176 = REGISTRY.register("scp1176", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp1176")));
	// SCP-902 is intentionally local and positional. A fixed range prevents the
	// replacement ticking/open/close samples from carrying through an entire
	// facility while level.playSound still provides normal 3D directionality.
	private static final float SCP902_SOUND_RANGE = 12.0F;
	public static final RegistryObject<SoundEvent> SCP902 = REGISTRY.register("scp902", () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("scp_classified_directive", "scp902"), SCP902_SOUND_RANGE));
	public static final RegistryObject<SoundEvent> SCP902CLOSING = REGISTRY.register("scp902closing", () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("scp_classified_directive", "scp902closing"), SCP902_SOUND_RANGE));
	public static final RegistryObject<SoundEvent> SCP902OPENING = REGISTRY.register("scp902opening", () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("scp_classified_directive", "scp902opening"), SCP902_SOUND_RANGE));
	public static final RegistryObject<SoundEvent> SCP079_2 = REGISTRY.register("scp079_2", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp079_2")));
	public static final RegistryObject<SoundEvent> BUTTON = REGISTRY.register("button", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "button")));
	public static final RegistryObject<SoundEvent> SCP914DOORCLOSE = REGISTRY.register("scp914doorclose", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914doorclose")));
	public static final RegistryObject<SoundEvent> SCP914DOOROPEN = REGISTRY.register("scp914dooropen", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914dooropen")));
	public static final RegistryObject<SoundEvent> SCP914KEY = REGISTRY.register("scp914key", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914key")));
	public static final RegistryObject<SoundEvent> SCP914REFINING = REGISTRY.register("scp914refining", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914refining")));
	public static final RegistryObject<SoundEvent> SCP914DIAL = REGISTRY.register("scp914dial", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914dial")));
	public static final RegistryObject<SoundEvent> SCP914INSIDE = REGISTRY.register("scp914inside", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914inside")));
	public static final RegistryObject<SoundEvent> SCP914DEATH = REGISTRY.register("scp914death", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp914death")));
	public static final RegistryObject<SoundEvent> SPRAY = REGISTRY.register("spray", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "spray")));
	public static final RegistryObject<SoundEvent> DECONTAMINATION = REGISTRY.register("decontamination", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "decontamination")));
	public static final RegistryObject<SoundEvent> DOOROPEN = REGISTRY.register("dooropen", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "dooropen")));
	public static final RegistryObject<SoundEvent> DOORCLOSING = REGISTRY.register("doorclosing", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "doorclosing")));
	public static final RegistryObject<SoundEvent> ACCESSGRANTED = REGISTRY.register("accessgranted", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "accessgranted")));
	public static final RegistryObject<SoundEvent> ACCESSDENIED = REGISTRY.register("accessdenied", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "accessdenied")));
	public static final RegistryObject<SoundEvent> SCP294ENTER = REGISTRY.register("scp294enter", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294enter")));
	public static final RegistryObject<SoundEvent> SCP294POURING = REGISTRY.register("scp294pouring", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294pouring")));
	public static final RegistryObject<SoundEvent> SCP294EMPTYCUP = REGISTRY.register("scp294emptycup", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294emptycup")));
	public static final RegistryObject<SoundEvent> SCP294OUTOFRANGE = REGISTRY.register("scp294outofrange", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294outofrange")));
	public static final RegistryObject<SoundEvent> SCP294ON = REGISTRY.register("scp294on", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294on")));
	public static final RegistryObject<SoundEvent> SCP294OFF = REGISTRY.register("scp294off", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294off")));
	public static final RegistryObject<SoundEvent> SCP294COINSLOT = REGISTRY.register("scp294coinslot", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp294coinslot")));
	public static final RegistryObject<SoundEvent> HEARTBEAT = REGISTRY.register("heartbeat", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "heartbeat")));
	public static final RegistryObject<SoundEvent> NUCLEAR = REGISTRY.register("nuclear", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "nuclear")));
	public static final RegistryObject<SoundEvent> GRAVITONS = REGISTRY.register("gravitons", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "gravitons")));
	public static final RegistryObject<SoundEvent> STOMACH = REGISTRY.register("stomach", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "stomach")));
	public static final RegistryObject<SoundEvent> HAZMAT_EQUIP = REGISTRY.register("hazmat_equip", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "hazmat_equip")));
	public static final RegistryObject<SoundEvent> HAZMAT_REMOVE = REGISTRY.register("hazmat_remove", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "hazmat_remove")));
	public static final RegistryObject<SoundEvent> HAZMAT_BREATHING = REGISTRY.register("hazmat_breathing", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "hazmat_breathing")));
	public static final RegistryObject<SoundEvent> SCP_714_MUSIC = REGISTRY.register("scp_714", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp_714")));
	public static final RegistryObject<SoundEvent> SCP012_TRANCE = REGISTRY.register("scp012_trance", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_trance")));
	public static final RegistryObject<SoundEvent> SCP012_DAMAGE = REGISTRY.register("scp012_damage", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_damage")));
	public static final RegistryObject<SoundEvent> SCP012_OPEN = REGISTRY.register("scp012_open", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_open")));
	public static final RegistryObject<SoundEvent> SCP012_CLOSE = REGISTRY.register("scp012_close", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_close")));
	public static final RegistryObject<SoundEvent> SCP012_BLEED_1 = REGISTRY.register("scp012_bleed_1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_bleed_1")));
	public static final RegistryObject<SoundEvent> SCP012_BLEED_2 = REGISTRY.register("scp012_bleed_2", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_bleed_2")));
	public static final RegistryObject<SoundEvent> SCP012_BLEED_3 = REGISTRY.register("scp012_bleed_3", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_bleed_3")));
	public static final RegistryObject<SoundEvent> SCP012_ON_MOUNT_GOLGOTHA = REGISTRY.register("scp012_on_mount_golgotha", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "scp012_on_mount_golgotha")));
	public static final RegistryObject<SoundEvent> PLAYER_HURT = REGISTRY.register("player_hurt", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "player_hurt")));
	public static final RegistryObject<SoundEvent> DAMAGE_SPLATTER = REGISTRY.register("damage_splatter", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "damage_splatter")));
	public static final RegistryObject<SoundEvent> LAMP_LOOP = REGISTRY.register("lamp_loop", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "lamp_loop")));
	public static final RegistryObject<SoundEvent> LAMP_ON = REGISTRY.register("lamp_on", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "lamp_on")));
	public static final RegistryObject<SoundEvent> LAMP_OFF = REGISTRY.register("lamp_off", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("scp_classified_directive", "lamp_off")));
}