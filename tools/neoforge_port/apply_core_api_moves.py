from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"


def path(relative: str) -> Path:
    return JAVA / relative


def write(relative: str, content: str) -> None:
    target = path(relative)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


# ArmorMaterial became a registered record in 1.21.1.
write("net/mcreator/scpadditions/init/ScpAdditionsModArmorMaterials.java", '''package net.mcreator.scpadditions.init;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Registered leather-equivalent material used by the hidden Hazmat proxies. */
public final class ScpAdditionsModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> REGISTRY =
            DeferredRegister.create(Registries.ARMOR_MATERIAL,
                    ScpAdditionsMod.MODID);

    public static final Holder<ArmorMaterial> HAZMAT = REGISTRY.register(
            "hazmat_suit", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 1);
                        defense.put(ArmorItem.Type.LEGGINGS, 2);
                        defense.put(ArmorItem.Type.CHESTPLATE, 3);
                        defense.put(ArmorItem.Type.HELMET, 1);
                        defense.put(ArmorItem.Type.BODY, 3);
                    }),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(
                                    ScpAdditionsMod.MODID, "hazmat_suit"))),
                    0.0F,
                    0.0F));

    private ScpAdditionsModArmorMaterials() {
    }
}
''')
path("net/mcreator/scpadditions/item/HazmatArmorMaterial.java").unlink(missing_ok=True)
armor_item = path("net/mcreator/scpadditions/item/HazmatArmorItem.java")
text = armor_item.read_text(encoding="utf-8")
if "ScpAdditionsModArmorMaterials" not in text:
    text = text.replace(
        "import net.mcreator.scpadditions.client.HazmatArmorRenderer;",
        "import net.mcreator.scpadditions.client.HazmatArmorRenderer;\n"
        "import net.mcreator.scpadditions.init.ScpAdditionsModArmorMaterials;")
text = text.replace("HazmatArmorMaterial.INSTANCE", "ScpAdditionsModArmorMaterials.HAZMAT")
armor_item.write_text(text, encoding="utf-8")

# NeoForge registry aliases replace the removed MissingMappingsEvent.
legacy_drinks = path("net/mcreator/scpadditions/item/LegacyDrinkItemMappings.java")
old = legacy_drinks.read_text(encoding="utf-8")
set_start = old.index("    private static final Set<String> LEGACY_DRINK_ITEMS")
set_end = old.index("\n\n    private LegacyDrinkItemMappings()", set_start)
set_block = old[set_start:set_end]
legacy_drinks.write_text('''package net.mcreator.scpadditions.item;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

/** Preserves old SCP-294 drink ids through NeoForge registry aliases. */
public final class LegacyDrinkItemMappings {
''' + set_block + '''

    private LegacyDrinkItemMappings() {
    }

    public static void registerAliases() {
        ResourceLocation replacement = ResourceLocation.fromNamespaceAndPath(
                ScpAdditionsMod.MODID, "cup_of_coffee");
        for (String oldPath : LEGACY_DRINK_ITEMS) {
            ScpAdditionsModItems.REGISTRY.addAlias(
                    ResourceLocation.fromNamespaceAndPath(
                            ScpAdditionsMod.MODID, oldPath),
                    replacement);
        }
    }
}
''', encoding="utf-8")

write("net/mcreator/scpadditions/facility/FacilityLegacyMappings.java", '''package net.mcreator.scpadditions.facility;

/** Registers standalone facility namespaces as aliases of scp_additions ids. */
public final class FacilityLegacyMappings {
    private FacilityLegacyMappings() {
    }

    public static void registerAliases() {
        FacilityModule.registerLegacyAliases();
        UBlocksModule.registerLegacyAliases();
    }
}
''')

facility = path("net/mcreator/scpadditions/facility/FacilityModule.java")
text = facility.read_text(encoding="utf-8")
if "public static void registerLegacyAliases()" not in text:
    text = text.replace(
'''    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);''',
'''    public static void registerLegacyAliases() {
        BLOCKS_BY_PATH.keySet().forEach(oldPath -> BLOCKS.addAlias(
                ResourceLocation.fromNamespaceAndPath(LEGACY_MODID, oldPath),
                ResourceLocation.fromNamespaceAndPath(MODID, oldPath)));
        ITEMS_BY_PATH.keySet().forEach(oldPath -> ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(LEGACY_MODID, oldPath),
                ResourceLocation.fromNamespaceAndPath(MODID, oldPath)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);''', 1)
facility.write_text(text, encoding="utf-8")

ublocks = path("net/mcreator/scpadditions/facility/UBlocksModule.java")
text = ublocks.read_text(encoding="utf-8")
if "import net.minecraft.resources.ResourceLocation;" not in text:
    text = text.replace("import net.minecraft.network.chat.Component;",
                        "import net.minecraft.network.chat.Component;\n"
                        "import net.minecraft.resources.ResourceLocation;")
if "public static void registerLegacyAliases()" not in text:
    text = text.replace(
'''    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);''',
'''    public static void registerLegacyAliases() {
        BLOCKS.getEntries().forEach(entry -> {
            String oldPath = entry.getId().getPath();
            BLOCKS.addAlias(
                    ResourceLocation.fromNamespaceAndPath(LEGACY_MODID, oldPath),
                    ResourceLocation.fromNamespaceAndPath(MODID, oldPath));
        });
        ITEMS.getEntries().forEach(entry -> {
            String oldPath = entry.getId().getPath();
            if (!isLegacyWallDetailPath(oldPath)) {
                ITEMS.addAlias(
                        ResourceLocation.fromNamespaceAndPath(LEGACY_MODID, oldPath),
                        ResourceLocation.fromNamespaceAndPath(MODID, oldPath));
            }
        });
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);''', 1)
ublocks.write_text(text, encoding="utf-8")

# Simple API moves.
replacements = {
    "net/mcreator/scpadditions/item/NamedSpawnEggItem.java": [
        ("net.neoforged.neoforge.common.ForgeSpawnEggItem",
         "net.neoforged.neoforge.common.DeferredSpawnEggItem"),
        ("extends ForgeSpawnEggItem", "extends DeferredSpawnEggItem"),
    ],
    "net/mcreator/scpadditions/init/ScpAdditionsModMenus.java": [
        ("IForgeMenuType", "IMenuTypeExtension"),
    ],
    "net/mcreator/scpadditions/init/ScpAdditionsModTabs.java": [
        ("import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;",
         "import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;"),
    ],
    "net/mcreator/scpadditions/effect/HazmatExternalEffectEvents.java": [
        ("LivingHurtEvent", "LivingIncomingDamageEvent"),
    ],
    "net/mcreator/scpadditions/event/Scp173DurabilityEvents.java": [
        ("LivingHurtEvent", "LivingIncomingDamageEvent"),
    ],
    "net/mcreator/scpadditions/facility/FacilityBlockMiningEvents.java": [
        ("net.neoforged.neoforge.common.ToolActions",
         "net.neoforged.neoforge.common.ItemAbilities"),
        ("ToolActions.PICKAXE_DIG", "ItemAbilities.PICKAXE_DIG"),
    ],
}
for relative, pairs in replacements.items():
    target = path(relative)
    text = target.read_text(encoding="utf-8")
    for before, after in pairs:
        text = text.replace(before, after)
    target.write_text(text, encoding="utf-8")

# Config screen extension point.
config = path("net/mcreator/scpadditions/config/ui/ConfigCenterClient.java")
text = config.read_text(encoding="utf-8")
text = text.replace("import net.minecraftforge.client.ConfigScreenHandler;\n",
                    "import net.neoforged.fml.ModList;\n"
                    "import net.neoforged.neoforge.client.gui.IConfigScreenFactory;\n")
text = text.replace("import net.minecraftforge.fml.ModLoadingContext;\n", "")
text = text.replace(
'''            event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(ConfigCenterClient::openFromMods)));''',
'''            event.enqueueWork(() -> ModList.get()
                    .getModContainerById(ScpAdditionsMod.MODID)
                    .ifPresent(container -> container.registerExtensionPoint(
                            IConfigScreenFactory.class,
                            () -> (ignored, parent) -> openFromMods(
                                    Minecraft.getInstance(), parent))));''')
config.write_text(text, encoding="utf-8")

# 1.21.1 GUI layers replace Forge's overlay registry.
vitals_mod = path("net/mcreator/scpadditions/vitals/client/ClientVitalsModEvents.java")
text = vitals_mod.read_text(encoding="utf-8").replace(
        "RegisterGuiOverlaysEvent", "RegisterGuiLayersEvent")
text = text.replace(
'''event.registerAboveAll("player_vitals_overlay",
                (gui, graphics, partialTick, width, height) ->
                        PlayerVitalsOverlay.render(graphics, width, height, partialTick));''',
'''event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        ScpAdditionsMod.MODID, "player_vitals_overlay"),
                (graphics, deltaTracker) -> PlayerVitalsOverlay.render(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));''')
vitals_mod.write_text(text, encoding="utf-8")

vitals = path("net/mcreator/scpadditions/vitals/client/ClientVitalsEvents.java")
text = vitals.read_text(encoding="utf-8")
text = text.replace("RenderGuiOverlayEvent", "RenderGuiLayerEvent")
text = text.replace("net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay",
                    "net.neoforged.neoforge.client.gui.VanillaGuiLayers")
text = text.replace("event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())",
                    "event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)")
vitals.write_text(text, encoding="utf-8")

scp173_client = path("net/mcreator/scpadditions/client/Scp173ClientModEvents.java")
text = scp173_client.read_text(encoding="utf-8").replace(
        "RegisterGuiOverlaysEvent", "RegisterGuiLayersEvent")
if "private static net.minecraft.resources.ResourceLocation id(String path)" not in text:
    match = re.search(
        r"    @SubscribeEvent\n    public static void registerOverlays\(RegisterGuiLayersEvent event\) \{.*?\n    \}\n\n    @SubscribeEvent\n    public static void registerKeys",
        text, re.S)
    if match:
        replacement = '''    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerBelowAll(id("player_view_effects_overlay"),
                (graphics, deltaTracker) -> {
                    int width = graphics.guiWidth();
                    int height = graphics.guiHeight();
                    float partialTick = deltaTracker
                            .getGameTimeDeltaPartialTick(false);
                    HazmatVisorOverlay.render(graphics, width, height);
                    Scp714VignetteOverlay.render(graphics, width, height,
                            partialTick);
                    Scp012SubliminalOverlay.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll(id("blink_vignette_overlay"),
                (graphics, deltaTracker) -> {
                    int width = graphics.guiWidth();
                    int height = graphics.guiHeight();
                    float partialTick = deltaTracker
                            .getGameTimeDeltaPartialTick(false);
                    BlinkClient.renderVignette(graphics, width, height);
                    Scp1176HoneyVignette.render(graphics, width, height,
                            partialTick);
                });
        event.registerAboveAll(id("blink_blackout_overlay"),
                (graphics, deltaTracker) -> BlinkClient.renderBlackout(
                        graphics, graphics.guiWidth(), graphics.guiHeight()));
        event.registerAboveAll(id("equipment_progress_overlay"),
                (graphics, deltaTracker) -> EquipmentProgressOverlay.render(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(id("blink_meter_overlay"),
                (graphics, deltaTracker) -> BlinkClient.renderHud(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAboveAll(id("scp_131_notice_overlay"),
                (graphics, deltaTracker) ->
                        Scp131NoticeOverlay.render(graphics));
    }

    private static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                ScpAdditionsMod.MODID, path);
    }

    @SubscribeEvent
    public static void registerKeys'''
        text = text[:match.start()] + replacement + text[match.end():]
scp173_client.write_text(text, encoding="utf-8")

# Living ticks are now standalone entity tick events.
eye = path("net/mcreator/scpadditions/effect/EyeSoreEffectEvents.java")
text = eye.read_text(encoding="utf-8")
text = text.replace("import net.neoforged.neoforge.event.entity.living.LivingEvent;",
                    "import net.neoforged.neoforge.event.tick.EntityTickEvent;")
text = text.replace("onLivingTick(LivingEvent.LivingTickEvent event)",
                    "onLivingTick(EntityTickEvent.Post event)")
text = text.replace("LivingEntity entity = event.getEntity();",
                    "if (!(event.getEntity() instanceof LivingEntity entity)) return;")
eye.write_text(text, encoding="utf-8")

# Attach aliases and the registered armor material before registry freeze.
mod = path("net/mcreator/scpadditions/ScpAdditionsMod.java")
text = mod.read_text(encoding="utf-8")
if "import net.mcreator.scpadditions.init.ScpAdditionsModArmorMaterials;" not in text:
    text = text.replace(
        "import net.mcreator.scpadditions.init.UnifiedReaderItems;",
        "import net.mcreator.scpadditions.init.UnifiedReaderItems;\n"
        "import net.mcreator.scpadditions.init.ScpAdditionsModArmorMaterials;\n"
        "import net.mcreator.scpadditions.item.LegacyDrinkItemMappings;\n"
        "import net.mcreator.scpadditions.facility.FacilityLegacyMappings;")
if "LegacyDrinkItemMappings.registerAliases();" not in text:
    text = text.replace(
'''        ScpAdditionsModBlocks.REGISTRY.register(bus);
        ScpAdditionsModBlockEntities.REGISTRY.register(bus);
        ScpAdditionsModItems.REGISTRY.register(bus);''',
'''        ScpAdditionsModBlocks.REGISTRY.register(bus);
        ScpAdditionsModBlockEntities.REGISTRY.register(bus);
        ScpAdditionsModArmorMaterials.REGISTRY.register(bus);
        LegacyDrinkItemMappings.registerAliases();
        FacilityLegacyMappings.registerAliases();
        ScpAdditionsModItems.REGISTRY.register(bus);''', 1)
mod.write_text(text, encoding="utf-8")

print("Applied NeoForge 1.21.1 core API migrations")
