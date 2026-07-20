from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"
changed: list[str] = []


def edit(rel: str, fn) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8")
    new = fn(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(rel)


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)


# NeoForge-only creative search bars have no equivalent builder extension on Fabric.
edit(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    lambda text: text.replace("\n                    .withSearchBar()", ""),
)


def patch_scp294_menu(text: str) -> str:
    if "LegacyMenuProvider" not in text:
        text = re.sub(
            r"(package [^;]+;\n)",
            r"\1\nimport net.mcreator.scpadditions.fabric.menu.LegacyMenuData;\n"
            r"import net.mcreator.scpadditions.fabric.menu.LegacyMenuProvider;\n",
            text,
            count=1,
        )
    pattern = re.compile(
        r"player\.openMenu\( new MenuProvider\(\) \{.*?\n\s*\}, pos\);",
        re.S,
    )
    replacement = '''player.openMenu(new LegacyMenuProvider(
\t\t\t\tComponent.literal("SCP-294"),
\t\t\t\t(id, inventory, menuPlayer, data) -> new Scp294GuiMenu(id, inventory, data.toBuffer()),
\t\t\t\t() -> LegacyMenuData.create(data -> data.writeBlockPos(pos))));'''
    text, count = pattern.subn(replacement, text)
    if count != 1 and "new LegacyMenuProvider(" not in text:
        raise RuntimeError("SCP-294 menu conversion failed")
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/block/Scp294Block.java",
    patch_scp294_menu,
)


def patch_saved_data(text: str) -> str:
    if "net.minecraft.util.datafix.DataFixTypes" not in text:
        text = re.sub(
            r"(package [^;]+;\n)",
            r"\1\nimport net.minecraft.util.datafix.DataFixTypes;\n",
            text,
            count=1,
        )
    return re.sub(
        r"new SavedData\.Factory<>\(([^,]+), ([^)]+)\)",
        r"new SavedData.Factory<>(\1, \2, DataFixTypes.LEVEL)",
        text,
    )


edit(
    "src/main/java/net/mcreator/scpadditions/network/ScpAdditionsModVariables.java",
    patch_saved_data,
)

write(
    "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModGameRules.java",
    '''package net.mcreator.scpadditions.init;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

public final class ScpAdditionsModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEON =
            GameRuleRegistry.register("teslaGateOn", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanValue> TESLAGATEMANUALOVERRIDE =
            GameRuleRegistry.register("teslaGateManualOverride", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.BooleanValue> SCP079CONTROLON =
            GameRuleRegistry.register("scp079controlOn", GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<GameRules.BooleanValue> DECONCHECKPOINT =
            GameRuleRegistry.register("deconCheckpoint", GameRules.Category.MISC,
                    GameRuleFactory.createBooleanRule(false));

    private ScpAdditionsModGameRules() {}

    public static void bootstrap() {}
}
''',
)


def patch_initializer(text: str) -> str:
    if "ScpAdditionsModGameRules" not in text:
        text = text.replace(
            "import net.mcreator.scpadditions.ScpAdditionsMod;",
            "import net.mcreator.scpadditions.ScpAdditionsMod;\n"
            "import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;\n"
            "import net.mcreator.scpadditions.init.ScpAdditionsModTabs;",
        )
    return text.replace(
        "    public void onInitialize() {\n        new ScpAdditionsMod(MOD_BUS);",
        "    public void onInitialize() {\n"
        "        ScpAdditionsModGameRules.bootstrap();\n"
        "        new ScpAdditionsMod(MOD_BUS);\n"
        "        ScpAdditionsModTabs.registerFabricEntries();",
    )


edit(
    "src/main/java/net/mcreator/scpadditions/fabric/ScpAdditionsFabric.java",
    patch_initializer,
)

for relative in [
    "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModScreens.java",
    "src/main/java/net/mcreator/scpadditions/facility/UBlocksClientEvents.java",
    "src/main/java/net/mcreator/scpadditions/facility/FacilityClientRenderEvents.java",
]:
    def patch_render_layers(text: str) -> str:
        return text.replace(
            "import net.minecraft.client.renderer.ItemBlockRenderTypes;",
            "import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;",
        ).replace(
            "ItemBlockRenderTypes.setRenderLayer(",
            "BlockRenderLayerMap.INSTANCE.putBlock(",
        )

    edit(relative, patch_render_layers)

write(
    "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModArmorMaterials.java",
    '''package net.mcreator.scpadditions.init;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> REGISTRY =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ScpAdditionsMod.MODID);

    public static final Holder<ArmorMaterial> HAZMAT = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "hazmat_suit"),
            new ArmorMaterial(
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
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            ScpAdditionsMod.MODID, "hazmat_suit"))),
                    0.0F,
                    0.0F));

    private ScpAdditionsModArmorMaterials() {}
}
''',
)


def patch_tabs(text: str) -> str:
    text = text.replace("import net.neoforged.fml.common.EventBusSubscriber;\n", "")
    text = text.replace("import net.neoforged.fml.common.Mod;\n", "")
    text = text.replace("import net.neoforged.bus.api.SubscribeEvent;\n", "")
    text = text.replace(
        "import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;\n",
        "",
    )
    if "FabricItemGroup" not in text:
        text = text.replace(
            "import java.util.function.Supplier;",
            "import java.util.function.Supplier;\n\n"
            "import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;\n"
            "import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;",
        )
    text = text.replace("@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)\n", "")
    text = text.replace("CreativeModeTab.builder()", "FabricItemGroup.builder()")
    text = text.replace(").withSearchBar().build())", ").build())")
    text = re.sub(
        r"\n\s*@SubscribeEvent\n\s*public static void buildTabContentsVanilla\(.*?\n\s*}\n}\s*$",
        '''

    private static boolean fabricEntriesRegistered;

    public static void registerFabricEntries() {
        if (fabricEntriesRegistered) return;
        fabricEntriesRegistered = true;
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(ScpAdditionsModItems.SCP_330_RED_CANDY.get());
            entries.accept(ScpAdditionsModItems.SCP_330_GREEN_CANDY.get());
            entries.accept(ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
            entries.accept(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get());
            entries.accept(ScpAdditionsModItems.SCP_1176HONEY.get());
        });
    }
}
''',
        text,
        flags=re.S,
    )
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModTabs.java",
    patch_tabs,
)


def patch_facility_items(text: str) -> str:
    old = "creativeItemsInDisplayOrder().forEach(item -> output.accept(item.get())))"
    new = '''creativeItemsInDisplayOrder().stream()
                                    .filter(item -> item.get() != BUTTON_CLOSED.get().asItem()
                                            && item.get() != BUTTON_LOCKED.get().asItem())
                                    .forEach(item -> output.accept(item.get())))'''
    return text.replace(old, new)


edit(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
    patch_facility_items,
)


def patch_particles(text: str) -> str:
    if "FabricParticleTypes" not in text:
        text = text.replace(
            "import java.util.function.Supplier;",
            "import java.util.function.Supplier;\n\n"
            "import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;",
        )
    return text.replace(
        "() -> new SimpleParticleType(false)",
        "() -> FabricParticleTypes.simple(false)",
    )


edit(
    "src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModParticleTypes.java",
    patch_particles,
)

edit(
    "src/main/java/net/neoforged/bus/api/Event.java",
    lambda text: text.replace(
        "public enum Result { DENY, DEFAULT, ALLOW }",
        "public enum Result { DENY, DEFAULT, ALLOW, DO_NOT_APPLY }",
    ),
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/MobEffectApplicableMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class MobEffectApplicableMixin {
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$checkEffect(
            MobEffectInstance instance,
            Entity source,
            CallbackInfoReturnable<Boolean> callback) {
        MobEffectEvent.Applicable event = new MobEffectEvent.Applicable(
                (LivingEntity) (Object) this, instance);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()
                || event.getResult() == Event.Result.DENY
                || event.getResult() == Event.Result.DO_NOT_APPLY) {
            callback.setReturnValue(false);
        }
    }
}
''',
)

write(
    "src/main/resources/scp_additions.accesswidener",
    '''accessWidener v2 named
accessible field net/minecraft/client/sounds/SoundEngine listener Lcom/mojang/blaze3d/audio/Listener;
accessible field net/minecraft/client/sounds/SoundEngine channelAccess Lnet/minecraft/client/sounds/ChannelAccess;
accessible field com/mojang/blaze3d/audio/Channel source I
''',
)


def patch_ublocks(text: str) -> str:
    text = text.replace(
        "entry.getId().getPath()",
        "BuiltInRegistries.BLOCK.getKey(entry.get()).getPath()",
        1,
    )
    text = text.replace(
        "entry.getId().getPath()",
        "BuiltInRegistries.ITEM.getKey(entry.get()).getPath()",
        1,
    )
    text = text.replace(
        "entry.getId().getPath().equals(path)",
        "BuiltInRegistries.BLOCK.getKey(entry.get()).getPath().equals(path)",
        1,
    )
    text = text.replace(
        "entry.getId().getPath().equals(path)",
        "BuiltInRegistries.ITEM.getKey(entry.get()).getPath().equals(path)",
        1,
    )
    return text


edit(
    "src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java",
    patch_ublocks,
)
edit(
    "src/main/java/net/mcreator/scpadditions/facility/DoorButtonPlacementEvents.java",
    lambda text: text.replace(
        "placed.getSoundType(level, pos, player)",
        "placed.getSoundType()",
    ),
)
edit(
    "src/main/java/net/mcreator/scpadditions/data/Scp914GenericRecipeResolver.java",
    lambda text: text.replace(
        "stack.hasCraftingRemainingItem()",
        "stack.getItem().hasCraftingRemainingItem()",
    ).replace(
        "stack.getCraftingRemainingItem()",
        "stack.getItem().getCraftingRemainingItem().getDefaultInstance()",
    ),
)

fabric_mod = RES / "fabric.mod.json"
metadata = json.loads(fabric_mod.read_text(encoding="utf-8"))
if metadata.get("mixins") != ["scp_additions.mixins.json"]:
    metadata["mixins"] = ["scp_additions.mixins.json"]
    fabric_mod.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    changed.append("src/main/resources/fabric.mod.json")

write(
    "src/main/resources/scp_additions.mixins.json",
    '''{
  "required": true,
  "package": "net.mcreator.scpadditions.fabric.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "MobEffectApplicableMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
''',
)

print(f"Fabric API round 5 changed {len(changed)} files")
for item in changed:
    print(item)
