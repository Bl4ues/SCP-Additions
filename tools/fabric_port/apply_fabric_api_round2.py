from __future__ import annotations

from pathlib import Path
import re
import textwrap

ROOT = Path.cwd()
JAVA = ROOT / "src/main/java"
if not JAVA.exists():
    ROOT = Path(__file__).resolve().parents[2]
    JAVA = ROOT / "src/main/java"


def write(rel: str, content: str) -> None:
    path = JAVA / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(textwrap.dedent(content).strip() + "\n", encoding="utf-8")


def rewrite(rel: str, transform) -> None:
    path = JAVA / rel
    source = path.read_text(encoding="utf-8")
    result = transform(source)
    if result != source:
        path.write_text(result, encoding="utf-8")

# ---------------------------------------------------------------------------
# Source-level loader adaptations
# ---------------------------------------------------------------------------

def rewrite_mod(source: str) -> str:
    source = source.replace("        ScpInventoryCapability.REGISTRY.register(bus);\n", "")
    source = source.replace("        ScpAdditionsModVariables.ATTACHMENTS.register(bus);\n", "")
    # Fabric's eager vanilla registration needs entity types before spawn eggs.
    source = source.replace(
        "        Scp714Items.REGISTRY.register(bus);\n"
        "        Scp012Module.register(bus);\n"
        "        UnifiedReaderItems.REGISTRY.register(bus);\n"
        "        Scp131Items.REGISTRY.register(bus);\n"
        "        ScpAdditionsModEntities.REGISTRY.register(bus);\n",
        "        ScpAdditionsModEntities.REGISTRY.register(bus);\n"
        "        Scp714Items.REGISTRY.register(bus);\n"
        "        Scp012Module.register(bus);\n"
        "        UnifiedReaderItems.REGISTRY.register(bus);\n"
        "        Scp131Items.REGISTRY.register(bus);\n",
    )
    return source

rewrite("net/mcreator/scpadditions/ScpAdditionsMod.java", rewrite_mod)

# Vanilla Fabric KeyMapping does not expose Forge's conflict-context constructor.
for rel in (
    "net/mcreator/scpadditions/client/Scp173Keybinds.java",
    "net/mcreator/scpadditions/client/Scp131Keybinds.java",
    "com/bl4ues/scpinventory/client/Keybinds.java",
):
    def key_transform(source: str) -> str:
        source = re.sub(r"^import net\.neoforged\.neoforge\.client\.settings\.KeyConflictContext;\s*\n", "", source, flags=re.MULTILINE)
        source = re.sub(r"\n\s*KeyConflictContext\.(?:IN_GAME|UNIVERSAL),", "", source)
        return source
    rewrite(rel, key_transform)

# Fabric provides its own client renderer registration path. Keep the methods as
# ordinary hooks for the Fabric client bootstrap instead of invalid overrides.
for rel in (
    "net/mcreator/scpadditions/item/HazmatArmorItem.java",
    "net/mcreator/scpadditions/effect/InventoryOnlyMobEffect.java",
    "net/mcreator/scpadditions/effect/Scp1176HoneyedEffect.java",
):
    def client_ext_transform(source: str) -> str:
        source = source.replace("    @Override\n    public void initializeClient", "    public void initializeClient")
        return source
    rewrite(rel, client_ext_transform)

# Replace patched NeoForge capability lookups with the Fabric-native bridge.
capability_replacements = {
    "itemstack.getCapability(\n                        Capabilities.ItemHandler.ITEM)": "net.mcreator.scpadditions.fabric.FabricItemHandlers.find(itemstack)",
    "boundEntity.getCapability(\n                            Capabilities.ItemHandler.ENTITY)": "net.mcreator.scpadditions.fabric.FabricItemHandlers.find(boundEntity)",
    "this.world.getCapability(\n                            Capabilities.ItemHandler.BLOCK, pos, null)": "net.mcreator.scpadditions.fabric.FabricItemHandlers.find(this.world, pos, null)",
    "entity.getCapability(Capabilities.ItemHandler.ENTITY)": "net.mcreator.scpadditions.fabric.FabricItemHandlers.find(entity)",
}
for rel in (
    "net/mcreator/scpadditions/world/inventory/Scp294GuiMenu.java",
    "net/mcreator/scpadditions/procedures/Scp1176OnBlockRightClickedProcedure.java",
):
    def cap_transform(source: str) -> str:
        for old, new in capability_replacements.items():
            source = source.replace(old, new)
        return source
    rewrite(rel, cap_transform)

# Item action checks are represented by vanilla tool components on Fabric.
rewrite(
    "net/mcreator/scpadditions/facility/FacilityBlockMiningEvents.java",
    lambda s: s.replace("import net.neoforged.neoforge.common.ItemAbilities;\n", "")
        .replace("tool.canPerformAction(ItemAbilities.PICKAXE_DIG)",
                 "net.mcreator.scpadditions.fabric.FabricItemAbilities.isPickaxe(tool)"),
)

# Direct Fabric attachment registration and persistent codecs.
write("com/bl4ues/scpinventory/capability/FabricScpInventoryCodec.java", r'''
package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FabricScpInventoryCodec {
    private static final Codec<Map<ScpEquipmentSlot, ItemStack>> EQUIPMENT_CODEC =
            Codec.unboundedMap(
                    Codec.STRING.xmap(ScpEquipmentSlot::valueOf, ScpEquipmentSlot::name),
                    ItemStack.OPTIONAL_CODEC);

    public static final Codec<IScpInventory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("max_main_slots", IScpInventory.DEFAULT_MAIN_SLOT_COUNT)
                    .forGetter(IScpInventory::getMaxMainSlots),
            Codec.INT.optionalFieldOf("coin_count", 0).forGetter(IScpInventory::getCoinCount),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("inventory", List.of())
                    .forGetter(IScpInventory::getInventory),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("keys", List.of())
                    .forGetter(IScpInventory::getKeys),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("documents", List.of())
                    .forGetter(IScpInventory::getDocuments),
            EQUIPMENT_CODEC.optionalFieldOf("equipment", Map.of())
                    .forGetter(FabricScpInventoryCodec::equipment),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("active_usable", ItemStack.EMPTY)
                    .forGetter(IScpInventory::getActiveUsable)
    ).apply(instance, FabricScpInventoryCodec::create));

    private FabricScpInventoryCodec() {}

    private static Map<ScpEquipmentSlot, ItemStack> equipment(IScpInventory inventory) {
        Map<ScpEquipmentSlot, ItemStack> result = new EnumMap<>(ScpEquipmentSlot.class);
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            ItemStack stack = inventory.getEquipment(slot);
            if (!stack.isEmpty()) result.put(slot, stack);
        }
        return result;
    }

    private static IScpInventory create(int maxSlots, int coins,
            List<ItemStack> inventory, List<ItemStack> keys,
            List<ItemStack> documents, Map<ScpEquipmentSlot, ItemStack> equipment,
            ItemStack activeUsable) {
        ScpInventory result = new ScpInventory();
        result.setMaxMainSlots(maxSlots);
        result.setCoinCount(coins);
        result.setInventory(new ArrayList<>(inventory));
        result.setKeys(new ArrayList<>(keys));
        result.setDocuments(new ArrayList<>(documents));
        equipment.forEach(result::setEquipment);
        result.setActiveUsable(activeUsable);
        return result;
    }
}
''')


def inventory_attachment(source: str) -> str:
    if "AttachmentRegistry.create(" in source:
        return source
    start = source.index("    public static final DeferredRegister<AttachmentType<?>> REGISTRY")
    end = source.index("    private ScpInventoryCapability()", start)
    replacement = '''    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<IScpInventory> INSTANCE =
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_inventory"),
                    builder -> builder.initializer(ScpInventory::new)
                            .persistent(FabricScpInventoryCodec.CODEC)
                            .copyOnDeath());

'''
    source = source[:start] + replacement + source[end:]
    source = re.sub(r"^import net\.neoforged\.neoforge\.(?:attachment|registries)[^;]+;\s*\n", "", source, flags=re.MULTILINE)
    source = source.replace("player.getData(INSTANCE)",
            "((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).getAttachedOrCreate(INSTANCE)")
    return source

rewrite("com/bl4ues/scpinventory/capability/ScpInventoryCapability.java", inventory_attachment)


def variables_attachment(source: str) -> str:
    if "AttachmentRegistry.create(" in source:
        return source
    start = source.index("\tpublic static final DeferredRegister<AttachmentType<?>> ATTACHMENTS")
    end = source.index("\n\tpublic static LazyOptional<PlayerVariables>", start)
    replacement = '''\tpublic static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<PlayerVariables> PLAYER_VARIABLES_ATTACHMENT =
\t\t\tnet.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
\t\t\t\tnet.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "player_variables"),
\t\t\t\tbuilder -> builder.initializer(PlayerVariables::new)
\t\t\t\t\t\t.persistent(net.minecraft.nbt.CompoundTag.CODEC.xmap(tag -> {
\t\t\t\t\t\t\tPlayerVariables variables = new PlayerVariables();
\t\t\t\t\t\t\tvariables.readNBT(tag);
\t\t\t\t\t\t\treturn variables;
\t\t\t\t\t\t}, variables -> (net.minecraft.nbt.CompoundTag) variables.writeNBT())));
'''
    source = source[:start] + replacement + source[end:]
    source = re.sub(r"^import net\.neoforged\.neoforge\.(?:attachment|registries)[^;]+;\s*\n", "", source, flags=re.MULTILINE)
    source = source.replace("player.getData(PLAYER_VARIABLES_ATTACHMENT)",
            "((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget) player).getAttachedOrCreate(PLAYER_VARIABLES_ATTACHMENT)")
    return source

rewrite("net/mcreator/scpadditions/network/ScpAdditionsModVariables.java", variables_attachment)

# ---------------------------------------------------------------------------
# Forge-style item handlers backed by vanilla containers
# ---------------------------------------------------------------------------
write("net/neoforged/neoforge/items/IItemHandler.java", r'''
package net.neoforged.neoforge.items;
import net.minecraft.world.item.ItemStack;
public interface IItemHandler {
    int getSlots();
    ItemStack getStackInSlot(int slot);
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
    ItemStack extractItem(int slot, int amount, boolean simulate);
    int getSlotLimit(int slot);
    boolean isItemValid(int slot, ItemStack stack);
}
''')
write("net/neoforged/neoforge/items/IItemHandlerModifiable.java", r'''
package net.neoforged.neoforge.items;
import net.minecraft.world.item.ItemStack;
public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, ItemStack stack);
}
''')
write("net/neoforged/neoforge/items/ItemStackHandler.java", r'''
package net.neoforged.neoforge.items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class ItemStackHandler implements IItemHandlerModifiable {
    protected final List<ItemStack> stacks;
    public ItemStackHandler(int size) {
        stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) stacks.add(ItemStack.EMPTY);
    }
    @Override public int getSlots() { return stacks.size(); }
    @Override public ItemStack getStackInSlot(int slot) { return valid(slot) ? stacks.get(slot) : ItemStack.EMPTY; }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        if (!valid(slot)) return;
        stacks.set(slot, stack == null ? ItemStack.EMPTY : stack);
        onContentsChanged(slot);
    }
    @Override public ItemStack insertItem(int slot, ItemStack incoming, boolean simulate) {
        if (!valid(slot) || incoming == null || incoming.isEmpty() || !isItemValid(slot, incoming)) return incoming;
        ItemStack current = stacks.get(slot);
        int limit = Math.min(getSlotLimit(slot), incoming.getMaxStackSize());
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, incoming)) return incoming;
        int room = limit - current.getCount();
        if (room <= 0) return incoming;
        int moved = Math.min(room, incoming.getCount());
        if (!simulate) {
            if (current.isEmpty()) {
                ItemStack inserted = incoming.copy();
                inserted.setCount(moved);
                stacks.set(slot, inserted);
            } else current.grow(moved);
            onContentsChanged(slot);
        }
        ItemStack remainder = incoming.copy();
        remainder.shrink(moved);
        return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!valid(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, current.getCount());
        ItemStack result = current.copy();
        result.setCount(moved);
        if (!simulate) {
            current.shrink(moved);
            if (current.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
        }
        return result;
    }
    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    protected void onContentsChanged(int slot) {}
    private boolean valid(int slot) { return slot >= 0 && slot < stacks.size(); }
}
''')
write("net/neoforged/neoforge/items/SlotItemHandler.java", r'''
package net.neoforged.neoforge.items;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotItemHandler extends Slot {
    private final IItemHandler handler;
    private final int index;
    public SlotItemHandler(IItemHandler handler, int index, int x, int y) {
        super(new SimpleContainer(Math.max(1, handler.getSlots())), index, x, y);
        this.handler = handler;
        this.index = index;
    }
    @Override public ItemStack getItem() { return handler.getStackInSlot(index); }
    @Override public boolean hasItem() { return !getItem().isEmpty(); }
    @Override public void set(ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) modifiable.setStackInSlot(index, stack);
        setChanged();
    }
    @Override public ItemStack remove(int amount) { return handler.extractItem(index, amount, false); }
    @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(index, stack); }
    @Override public int getMaxStackSize() { return handler.getSlotLimit(index); }
    @Override public void onTake(Player player, ItemStack stack) { setChanged(); }
}
''')
write("net/neoforged/neoforge/items/wrapper/SidedInvWrapper.java", r'''
package net.neoforged.neoforge.items.wrapper;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class SidedInvWrapper implements IItemHandlerModifiable {
    private final Container container;
    private final Direction side;
    public SidedInvWrapper(Container container, Direction side) {
        this.container = container;
        this.side = side;
    }
    private int[] slots() {
        if (container instanceof WorldlyContainer worldly && side != null) return worldly.getSlotsForFace(side);
        int[] result = new int[container.getContainerSize()];
        for (int i = 0; i < result.length; i++) result[i] = i;
        return result;
    }
    private int resolve(int slot) { int[] slots = slots(); return slot >= 0 && slot < slots.length ? slots[slot] : -1; }
    @Override public int getSlots() { return slots().length; }
    @Override public ItemStack getStackInSlot(int slot) { int actual = resolve(slot); return actual < 0 ? ItemStack.EMPTY : container.getItem(actual); }
    @Override public void setStackInSlot(int slot, ItemStack stack) { int actual = resolve(slot); if (actual >= 0) { container.setItem(actual, stack); container.setChanged(); } }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int actual = resolve(slot);
        if (actual < 0 || stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        ItemStack current = container.getItem(actual);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        int room = limit - current.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return stack;
        if (!simulate) {
            if (current.isEmpty()) { ItemStack copy = stack.copy(); copy.setCount(moved); container.setItem(actual, copy); }
            else current.grow(moved);
            container.setChanged();
        }
        ItemStack remainder = stack.copy(); remainder.shrink(moved); return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int actual = resolve(slot);
        if (actual < 0 || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = container.getItem(actual);
        if (current.isEmpty()) return ItemStack.EMPTY;
        if (container instanceof WorldlyContainer worldly && side != null && !worldly.canTakeItemThroughFace(actual, current, side)) return ItemStack.EMPTY;
        int moved = Math.min(amount, current.getCount());
        ItemStack result = current.copy(); result.setCount(moved);
        if (!simulate) { current.shrink(moved); if (current.isEmpty()) container.setItem(actual, ItemStack.EMPTY); container.setChanged(); }
        return result;
    }
    @Override public int getSlotLimit(int slot) { return container.getMaxStackSize(); }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        int actual = resolve(slot);
        if (actual < 0 || !container.canPlaceItem(actual, stack)) return false;
        return !(container instanceof WorldlyContainer worldly) || side == null || worldly.canPlaceItemThroughFace(actual, stack, side);
    }
}
''')
write("net/neoforged/neoforge/items/ItemHandlerHelper.java", r'''
package net.neoforged.neoforge.items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class ItemHandlerHelper {
    private ItemHandlerHelper() {}
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) player.drop(copy, false);
    }
}
''')

write("net/mcreator/scpadditions/fabric/FabricItemHandlers.java", r'''
package net.mcreator.scpadditions.fabric;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class FabricItemHandlers {
    private FabricItemHandlers() {}
    public static IItemHandler find(ItemStack stack) { return null; }
    public static IItemHandler find(Entity entity) {
        if (entity instanceof Player player) return new SidedInvWrapper(player.getInventory(), null);
        return entity instanceof Container container ? new SidedInvWrapper(container, null) : null;
    }
    public static IItemHandler find(Level level, BlockPos pos, @Nullable Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        try {
            Method method = blockEntity.getClass().getMethod("getItemHandler", Direction.class);
            return (IItemHandler) method.invoke(blockEntity, side);
        } catch (ReflectiveOperationException ignored) {
            return blockEntity instanceof Container container ? new SidedInvWrapper(container, side) : null;
        }
    }
}
''')
write("net/mcreator/scpadditions/fabric/FabricItemAbilities.java", r'''
package net.mcreator.scpadditions.fabric;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
public final class FabricItemAbilities {
    private FabricItemAbilities() {}
    public static boolean isPickaxe(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.has(DataComponents.TOOL);
    }
}
''')

# Capability registration remains a harmless compatibility event; actual block
# lookups are served by FabricItemHandlers.
write("net/neoforged/neoforge/capabilities/Capabilities.java", r'''
package net.neoforged.neoforge.capabilities;
public final class Capabilities {
    private Capabilities() {}
    public static final class ItemHandler {
        public static final Object BLOCK = new Object();
        public static final Object ENTITY = new Object();
        public static final Object ITEM = new Object();
        private ItemHandler() {}
    }
}
''')
write("net/neoforged/neoforge/capabilities/RegisterCapabilitiesEvent.java", r'''
package net.neoforged.neoforge.capabilities;
import java.util.function.BiFunction;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
public class RegisterCapabilitiesEvent extends Event {
    public <T extends BlockEntity, C> void registerBlockEntity(Object capability,
            BlockEntityType<T> type, BiFunction<T, Direction, C> provider) {}
}
''')

# Utility compatibility types.
write("net/neoforged/neoforge/common/util/TriState.java", r'''
package net.neoforged.neoforge.common.util;
public enum TriState { TRUE, DEFAULT, FALSE }
''')
write("net/neoforged/neoforge/common/util/FakePlayer.java", r'''
package net.neoforged.neoforge.common.util;
public interface FakePlayer {}
''')
write("net/neoforged/neoforge/common/DeferredSpawnEggItem.java", r'''
package net.neoforged.neoforge.common;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
public class DeferredSpawnEggItem extends SpawnEggItem {
    public DeferredSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type,
            int backgroundColor, int highlightColor, Properties properties) {
        super(type.get(), backgroundColor, highlightColor, properties);
    }
}
''')
write("net/neoforged/neoforge/client/settings/KeyConflictContext.java", r'''
package net.neoforged.neoforge.client.settings;
public enum KeyConflictContext { IN_GAME, UNIVERSAL }
''')
write("net/neoforged/neoforge/client/gui/VanillaGuiLayers.java", r'''
package net.neoforged.neoforge.client.gui;
import net.minecraft.resources.ResourceLocation;
public final class VanillaGuiLayers {
    public static final ResourceLocation PLAYER_HEALTH = ResourceLocation.withDefaultNamespace("player_health");
    private VanillaGuiLayers() {}
}
''')
write("net/neoforged/neoforge/common/extensions/IMenuTypeExtension.java", r'''
package net.neoforged.neoforge.common.extensions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
public final class IMenuTypeExtension {
    private IMenuTypeExtension() {}
    @FunctionalInterface public interface Factory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory, FriendlyByteBuf data);
    }
    public static <T extends AbstractContainerMenu> MenuType<T> create(Factory<T> factory) {
        return new MenuType<>((syncId, inventory) -> factory.create(syncId, inventory, null), FeatureFlags.DEFAULT_FLAGS);
    }
}
''')
write("net/neoforged/neoforge/client/extensions/common/IClientItemExtensions.java", r'''
package net.neoforged.neoforge.client.extensions.common;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
public interface IClientItemExtensions {
    default HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
            EquipmentSlot slot, HumanoidModel<?> original) { return original; }
}
''')
write("net/neoforged/neoforge/client/extensions/common/IClientMobEffectExtensions.java", r'''
package net.neoforged.neoforge.client.extensions.common;
import net.minecraft.world.effect.MobEffectInstance;
public interface IClientMobEffectExtensions {
    default boolean isVisibleInInventory(MobEffectInstance instance) { return true; }
    default boolean isVisibleInGui(MobEffectInstance instance) { return true; }
}
''')

# ---------------------------------------------------------------------------
# Event compatibility surface
# ---------------------------------------------------------------------------
write("net/neoforged/neoforge/event/tick/ServerTickEvent.java", r'''
package net.neoforged.neoforge.event.tick;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
public abstract class ServerTickEvent extends Event {
    private final MinecraftServer server;
    protected ServerTickEvent(MinecraftServer server) { this.server = server; }
    public MinecraftServer getServer() { return server; }
    public static final class Pre extends ServerTickEvent { public Pre(MinecraftServer server) { super(server); } }
    public static final class Post extends ServerTickEvent { public Post(MinecraftServer server) { super(server); } }
}
''')
write("net/neoforged/neoforge/event/tick/LevelTickEvent.java", r'''
package net.neoforged.neoforge.event.tick;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
public abstract class LevelTickEvent extends Event {
    private final Level level;
    protected LevelTickEvent(Level level) { this.level = level; }
    public Level getLevel() { return level; }
    public static final class Pre extends LevelTickEvent { public Pre(Level level) { super(level); } }
    public static final class Post extends LevelTickEvent { public Post(Level level) { super(level); } }
}
''')
write("net/neoforged/neoforge/event/tick/PlayerTickEvent.java", r'''
package net.neoforged.neoforge.event.tick;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public abstract class PlayerTickEvent extends Event {
    private final Player entity;
    protected PlayerTickEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static final class Pre extends PlayerTickEvent { public Pre(Player player) { super(player); } }
    public static final class Post extends PlayerTickEvent { public Post(Player player) { super(player); } }
}
''')
write("net/neoforged/neoforge/event/tick/EntityTickEvent.java", r'''
package net.neoforged.neoforge.event.tick;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
public abstract class EntityTickEvent extends Event {
    private final Entity entity;
    protected EntityTickEvent(Entity entity) { this.entity = entity; }
    public Entity getEntity() { return entity; }
    public static final class Post extends EntityTickEvent { public Post(Entity entity) { super(entity); } }
}
''')

write("net/neoforged/neoforge/event/entity/player/PlayerEvent.java", r'''
package net.neoforged.neoforge.event.entity.player;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

public class PlayerEvent extends Event {
    private final Player entity;
    protected PlayerEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static class PlayerLoggedInEvent extends PlayerEvent { public PlayerLoggedInEvent(Player p) { super(p); } }
    public static class PlayerLoggedOutEvent extends PlayerEvent { public PlayerLoggedOutEvent(Player p) { super(p); } }
    public static class PlayerRespawnEvent extends PlayerEvent { public PlayerRespawnEvent(Player p) { super(p); } }
    public static class PlayerChangedDimensionEvent extends PlayerEvent { public PlayerChangedDimensionEvent(Player p) { super(p); } }
    public static class Clone extends PlayerEvent {
        private final Player original; private final boolean wasDeath;
        public Clone(Player clone, Player original, boolean wasDeath) { super(clone); this.original = original; this.wasDeath = wasDeath; }
        public Player getOriginal() { return original; }
        public boolean isWasDeath() { return wasDeath; }
    }
    public static class StartTracking extends PlayerEvent {
        private final Entity target;
        public StartTracking(Player player, Entity target) { super(player); this.target = target; }
        public Entity getTarget() { return target; }
    }
    public static class ItemCraftedEvent extends PlayerEvent {
        private final Container inventory;
        public ItemCraftedEvent(Player player, Container inventory) { super(player); this.inventory = inventory; }
        public Container getInventory() { return inventory; }
    }
    public static class BreakSpeed extends PlayerEvent {
        private final BlockState state; private final Optional<BlockPos> position;
        private final float originalSpeed; private float newSpeed;
        public BreakSpeed(Player player, BlockState state, Optional<BlockPos> position, float originalSpeed) {
            super(player); this.state = state; this.position = position; this.originalSpeed = originalSpeed; this.newSpeed = originalSpeed;
        }
        public BlockState getState() { return state; }
        public Optional<BlockPos> getPosition() { return position; }
        public float getOriginalSpeed() { return originalSpeed; }
        public float getNewSpeed() { return newSpeed; }
        public void setNewSpeed(float speed) { this.newSpeed = speed; }
    }
}
''')
write("net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java", r'''
package net.neoforged.neoforge.event.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.TriState;

public class PlayerInteractEvent extends Event {
    private final Player entity;
    protected PlayerInteractEvent(Player entity) { this.entity = entity; }
    public Player getEntity() { return entity; }
    public static final class RightClickBlock extends PlayerInteractEvent {
        private final InteractionHand hand; private final BlockHitResult hit;
        private InteractionResult cancellationResult = InteractionResult.PASS;
        private TriState useBlock = TriState.DEFAULT, useItem = TriState.DEFAULT;
        public RightClickBlock(Player player, InteractionHand hand, BlockHitResult hit) { super(player); this.hand = hand; this.hit = hit; }
        public InteractionHand getHand() { return hand; }
        public Level getLevel() { return getEntity().level(); }
        public BlockPos getPos() { return hit.getBlockPos(); }
        public Direction getFace() { return hit.getDirection(); }
        public BlockHitResult getHitVec() { return hit; }
        public ItemStack getItemStack() { return getEntity().getItemInHand(hand); }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
        public void setUseBlock(TriState state) { useBlock = state; }
        public void setUseItem(TriState state) { useItem = state; }
        public TriState getUseBlock() { return useBlock; }
        public TriState getUseItem() { return useItem; }
    }
    public static final class RightClickItem extends PlayerInteractEvent {
        private final InteractionHand hand; private InteractionResult cancellationResult = InteractionResult.PASS;
        public RightClickItem(Player player, InteractionHand hand) { super(player); this.hand = hand; }
        public Level getLevel() { return getEntity().level(); }
        public ItemStack getItemStack() { return getEntity().getItemInHand(hand); }
        public InteractionHand getHand() { return hand; }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
    }
    public static final class EntityInteract extends PlayerInteractEvent {
        private final Entity target; private InteractionResult cancellationResult = InteractionResult.PASS;
        public EntityInteract(Player player, Entity target) { super(player); this.target = target; }
        public Entity getTarget() { return target; }
        public void setCancellationResult(InteractionResult result) { cancellationResult = result; }
        public InteractionResult getCancellationResult() { return cancellationResult; }
    }
}
''')
write("net/neoforged/neoforge/event/entity/player/ItemTooltipEvent.java", r'''
package net.neoforged.neoforge.event.entity.player;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class ItemTooltipEvent extends Event {
    private final ItemStack stack; private final List<Component> tooltip;
    public ItemTooltipEvent(ItemStack stack, List<Component> tooltip) { this.stack = stack; this.tooltip = tooltip; }
    public ItemStack getItemStack() { return stack; }
    public List<Component> getToolTip() { return tooltip; }
}
''')
write("net/neoforged/neoforge/event/entity/player/ItemEntityPickupEvent.java", r'''
package net.neoforged.neoforge.event.entity.player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.TriState;
public final class ItemEntityPickupEvent {
    private ItemEntityPickupEvent() {}
    public static class Pre extends Event {
        private final Player player; private final ItemEntity item; private TriState canPickup = TriState.DEFAULT;
        public Pre(Player player, ItemEntity item) { this.player = player; this.item = item; }
        public Player getPlayer() { return player; }
        public ItemEntity getItemEntity() { return item; }
        public void setCanPickup(TriState state) { canPickup = state; }
        public TriState getCanPickup() { return canPickup; }
    }
}
''')
write("net/neoforged/neoforge/event/entity/item/ItemTossEvent.java", r'''
package net.neoforged.neoforge.event.entity.item;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
public class ItemTossEvent extends Event {
    private final ItemEntity entity; private final Player player;
    public ItemTossEvent(ItemEntity entity, Player player) { this.entity = entity; this.player = player; }
    public ItemEntity getEntity() { return entity; }
    public Player getPlayer() { return player; }
}
''')
write("net/neoforged/neoforge/event/level/BlockEvent.java", r'''
package net.neoforged.neoforge.event.level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
public class BlockEvent extends Event {
    private final LevelAccessor level; private final BlockPos pos; private final BlockState state;
    protected BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) { this.level=level; this.pos=pos; this.state=state; }
    public LevelAccessor getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockState getState() { return state; }
    public static class BreakEvent extends BlockEvent {
        private final Player player;
        public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) { super(level,pos,state); this.player=player; }
        public Player getPlayer() { return player; }
    }
    public static class EntityPlaceEvent extends BlockEvent {
        private final Entity entity;
        public EntityPlaceEvent(LevelAccessor level, BlockPos pos, BlockState state, Entity entity) { super(level,pos,state); this.entity=entity; }
        public Entity getEntity() { return entity; }
        public BlockState getPlacedBlock() { return getState(); }
    }
    public static class NeighborNotifyEvent extends BlockEvent {
        public NeighborNotifyEvent(LevelAccessor level, BlockPos pos, BlockState state) { super(level,pos,state); }
    }
}
''')
write("net/neoforged/neoforge/event/entity/EntityJoinLevelEvent.java", r'''
package net.neoforged.neoforge.event.entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
public class EntityJoinLevelEvent extends Event {
    private final Entity entity; private final Level level;
    public EntityJoinLevelEvent(Entity entity, Level level) { this.entity=entity; this.level=level; }
    public Entity getEntity() { return entity; }
    public Level getLevel() { return level; }
}
''')
write("net/neoforged/neoforge/event/entity/ProjectileImpactEvent.java", r'''
package net.neoforged.neoforge.event.entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
public class ProjectileImpactEvent extends Event {
    private final Projectile projectile; private final HitResult hit;
    public ProjectileImpactEvent(Projectile projectile, HitResult hit) { this.projectile=projectile; this.hit=hit; }
    public Projectile getProjectile() { return projectile; }
    public HitResult getRayTraceResult() { return hit; }
}
''')
write("net/neoforged/neoforge/event/entity/living/LivingIncomingDamageEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public class LivingIncomingDamageEvent extends Event {
    private final LivingEntity entity; private final DamageSource source; private float amount;
    public LivingIncomingDamageEvent(LivingEntity entity, DamageSource source, float amount) { this.entity=entity; this.source=source; this.amount=amount; }
    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount=amount; }
}
''')
write("net/neoforged/neoforge/event/entity/living/LivingDeathEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public class LivingDeathEvent extends Event {
    private final LivingEntity entity; private final DamageSource source;
    public LivingDeathEvent(LivingEntity entity, DamageSource source) { this.entity=entity; this.source=source; }
    public LivingEntity getEntity() { return entity; }
    public DamageSource getSource() { return source; }
}
''')
write("net/neoforged/neoforge/event/entity/living/LivingDropsEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import java.util.Collection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.Event;
public class LivingDropsEvent extends Event {
    private final LivingEntity entity; private final Collection<ItemEntity> drops;
    public LivingDropsEvent(LivingEntity entity, Collection<ItemEntity> drops) { this.entity=entity; this.drops=drops; }
    public LivingEntity getEntity() { return entity; }
    public Collection<ItemEntity> getDrops() { return drops; }
}
''')
write("net/neoforged/neoforge/event/entity/living/LivingHealEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public class LivingHealEvent extends Event {
    private final LivingEntity entity; private float amount;
    public LivingHealEvent(LivingEntity entity, float amount) { this.entity=entity; this.amount=amount; }
    public LivingEntity getEntity() { return entity; }
    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount=amount; }
}
''')
write("net/neoforged/neoforge/event/entity/living/LivingEntityUseItemEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class LivingEntityUseItemEvent extends Event {
    private final LivingEntity entity; private final ItemStack item;
    protected LivingEntityUseItemEvent(LivingEntity entity, ItemStack item) { this.entity=entity; this.item=item; }
    public LivingEntity getEntity() { return entity; }
    public ItemStack getItem() { return item; }
    public static class Start extends LivingEntityUseItemEvent { public Start(LivingEntity e, ItemStack s) { super(e,s); } }
    public static class Finish extends LivingEntityUseItemEvent { public Finish(LivingEntity e, ItemStack s) { super(e,s); } }
}
''')
write("net/neoforged/neoforge/event/entity/living/MobEffectEvent.java", r'''
package net.neoforged.neoforge.event.entity.living;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
public final class MobEffectEvent {
    private MobEffectEvent() {}
    public static class Applicable extends Event {
        private final LivingEntity entity; private final MobEffectInstance instance;
        public Applicable(LivingEntity entity, MobEffectInstance instance) { this.entity=entity; this.instance=instance; }
        public LivingEntity getEntity() { return entity; }
        public MobEffectInstance getEffectInstance() { return instance; }
    }
}
''')
write("net/neoforged/neoforge/event/RegisterCommandsEvent.java", r'''
package net.neoforged.neoforge.event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.Event;
public class RegisterCommandsEvent extends Event {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher) { this.dispatcher=dispatcher; }
    public CommandDispatcher<CommandSourceStack> getDispatcher() { return dispatcher; }
}
''')
write("net/neoforged/neoforge/event/entity/EntityAttributeCreationEvent.java", r'''
package net.neoforged.neoforge.event.entity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.Event;
public class EntityAttributeCreationEvent extends Event {
    public <T extends LivingEntity> void put(EntityType<T> type, AttributeSupplier attributes) {
        FabricDefaultAttributeRegistry.register(type, attributes);
    }
}
''')
write("net/neoforged/neoforge/event/BuildCreativeModeTabContentsEvent.java", r'''
package net.neoforged.neoforge.event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
public class BuildCreativeModeTabContentsEvent extends Event {
    private final ResourceKey<CreativeModeTab> key; private final CreativeModeTab tab;
    private final CreativeModeTab.Output output;
    public BuildCreativeModeTabContentsEvent(ResourceKey<CreativeModeTab> key, CreativeModeTab tab, CreativeModeTab.Output output) { this.key=key; this.tab=tab; this.output=output; }
    public ResourceKey<CreativeModeTab> getTabKey() { return key; }
    public CreativeModeTab getTab() { return tab; }
    public void accept(ItemLike item) { output.accept(item); }
    public void accept(ItemStack stack) { output.accept(stack); }
    public void remove(ItemStack stack, CreativeModeTab.TabVisibility visibility) {}
}
''')
write("net/neoforged/neoforge/registries/RegisterEvent.java", r'''
package net.neoforged.neoforge.registries;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
public class RegisterEvent extends Event {
    public <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> supplier) {
        @SuppressWarnings("unchecked") Registry<T> registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key.location());
        Registry.register(registry, id, supplier.get());
    }
}
''')

# Network registration compatibility shells are replaced by the Fabric-backed
# SimpleChannel below; these types remain only for subscriber signatures.
write("net/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent.java", r'''
package net.neoforged.neoforge.network.event;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
public class RegisterPayloadHandlersEvent extends Event {
    public PayloadRegistrar registrar(String version) { return new PayloadRegistrar(); }
}
''')
write("net/neoforged/neoforge/network/registration/PayloadRegistrar.java", r'''
package net.neoforged.neoforge.network.registration;
public class PayloadRegistrar {
    public PayloadRegistrar optional() { return this; }
    public <T> void playBidirectional(Object type, Object codec, Object handler) {}
}
''')
write("net/neoforged/neoforge/network/handling/IPayloadContext.java", r'''
package net.neoforged.neoforge.network.handling;
public interface IPayloadContext {}
''')
write("net/neoforged/neoforge/server/ServerLifecycleHooks.java", r'''
package net.neoforged.neoforge.server;
import net.minecraft.server.MinecraftServer;
public final class ServerLifecycleHooks {
    private static volatile MinecraftServer current;
    private ServerLifecycleHooks() {}
    public static MinecraftServer getCurrentServer() { return current; }
    public static void setCurrentServer(MinecraftServer server) { current = server; }
}
''')

# Fabric-native legacy channel implementation.
write("com/bl4ues/scpadditions/compat/network/NetworkEvent.java", r'''
package com.bl4ues.scpadditions.compat.network;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkEvent {
    private NetworkEvent() {}
    public static final class Context {
        private final Executor executor; private final ServerPlayer sender; private final boolean serverReception;
        Context(Executor executor, ServerPlayer sender, boolean serverReception) { this.executor=executor; this.sender=sender; this.serverReception=serverReception; }
        public CompletableFuture<Void> enqueueWork(Runnable task) { return CompletableFuture.runAsync(task, executor); }
        public ServerPlayer getSender() { return sender; }
        public Direction getDirection() { return new Direction(serverReception); }
        public void setPacketHandled(boolean handled) {}
    }
    public static final class Direction {
        private final boolean server;
        private Direction(boolean server) { this.server=server; }
        public ReceptionSide getReceptionSide() { return new ReceptionSide(server); }
    }
    public static final class ReceptionSide {
        private final boolean server;
        private ReceptionSide(boolean server) { this.server=server; }
        public boolean isServer() { return server; }
        public boolean isClient() { return !server; }
    }
}
''')
write("com/bl4ues/scpadditions/compat/network/SimpleChannel.java", r'''
package com.bl4ues.scpadditions.compat.network;

import java.util.*;
import java.util.function.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class SimpleChannel {
    private static final List<SimpleChannel> CHANNELS = new ArrayList<>();
    private final ResourceLocation channelId;
    private final String version;
    private final CustomPacketPayload.Type<Envelope> payloadType;
    private final Map<Integer, Registration<?>> byId = new LinkedHashMap<>();
    private final Map<Class<?>, Registration<?>> byClass = new LinkedHashMap<>();
    private boolean commonRegistered, clientRegistered;
    private final StreamCodec<RegistryFriendlyByteBuf, Envelope> codec = new StreamCodec<>() {
        @Override public Envelope decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt(); Registration<?> registration = require(id);
            return new Envelope(SimpleChannel.this, id, registration.decode(buffer));
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, Envelope envelope) {
            buffer.writeVarInt(envelope.messageId); require(envelope.messageId).encode(envelope.message, buffer);
        }
    };
    SimpleChannel(ResourceLocation channelId, String version) {
        this.channelId=channelId; this.version=version; this.payloadType=new CustomPacketPayload.Type<>(channelId);
        synchronized (CHANNELS) { CHANNELS.add(this); }
    }
    public synchronized <T> void registerMessage(int id, Class<T> type,
            BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf,T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> handler) {
        Registration<T> registration = new Registration<>(id,type,encoder,decoder,handler);
        byId.put(id,registration); byClass.put(type,registration);
    }
    public void send(PacketDistributor.PacketTarget target, Object message) {
        Envelope payload=envelope(message);
        switch (target.kind()) {
            case PLAYER -> ServerPlayNetworking.send((ServerPlayer)target.value(), payload);
            case ALL -> {
                ServerPlayer player=(ServerPlayer)firstServerPlayer();
                if (player != null) for (ServerPlayer targetPlayer: PlayerLookup.all(player.server)) ServerPlayNetworking.send(targetPlayer,payload);
            }
            case DIMENSION -> {
                @SuppressWarnings("unchecked") ResourceKey<Level> key=(ResourceKey<Level>)target.value();
                ServerPlayer player=(ServerPlayer)firstServerPlayer();
                if (player != null) { ServerLevel level=player.server.getLevel(key); if (level != null) for(ServerPlayer p:PlayerLookup.world(level)) ServerPlayNetworking.send(p,payload); }
            }
            case TRACKING_ENTITY -> { for(ServerPlayer p:PlayerLookup.tracking((Entity)target.value())) ServerPlayNetworking.send(p,payload); }
            case TRACKING_ENTITY_AND_SELF -> {
                Entity entity=(Entity)target.value(); for(ServerPlayer p:PlayerLookup.tracking(entity)) ServerPlayNetworking.send(p,payload);
                if(entity instanceof ServerPlayer self) ServerPlayNetworking.send(self,payload);
            }
        }
    }
    private Object firstServerPlayer() {
        for (ServerPlayer player : net.mcreator.scpadditions.fabric.FabricServerContext.players()) return player;
        return null;
    }
    public void sendToServer(Object message) { ClientPlayNetworking.send(envelope(message)); }
    private Envelope envelope(Object message) { Registration<?> registration=byClass.get(message.getClass()); if(registration==null) throw new IllegalStateException("Unregistered packet "+message.getClass()); return new Envelope(this,registration.id,message); }
    private Registration<?> require(int id) { Registration<?> registration=byId.get(id); if(registration==null) throw new IllegalStateException("Unknown packet "+id+" on "+channelId); return registration; }
    public synchronized void registerCommon() {
        if(commonRegistered) return; commonRegistered=true;
        PayloadTypeRegistry.playC2S().register(payloadType,codec);
        PayloadTypeRegistry.playS2C().register(payloadType,codec);
        ServerPlayNetworking.registerGlobalReceiver(payloadType,(payload,context)->require(payload.messageId).handle(payload.message,new NetworkEvent.Context(context.server(),context.player(),true)));
    }
    public synchronized void registerClient() {
        if(clientRegistered) return; clientRegistered=true;
        ClientPlayNetworking.registerGlobalReceiver(payloadType,(payload,context)->require(payload.messageId).handle(payload.message,new NetworkEvent.Context(context.client(),null,false)));
    }
    public static void registerAllCommon() { synchronized(CHANNELS){ CHANNELS.forEach(SimpleChannel::registerCommon); } }
    public static void registerAllClient() { synchronized(CHANNELS){ CHANNELS.forEach(SimpleChannel::registerClient); } }
    String version() { return version; }
    static List<SimpleChannel> channels() { synchronized(CHANNELS){ return List.copyOf(CHANNELS); } }
    public static final class Envelope implements CustomPacketPayload {
        private final SimpleChannel channel; private final int messageId; private final Object message;
        private Envelope(SimpleChannel channel,int id,Object message){this.channel=channel;this.messageId=id;this.message=message;}
        @Override public Type<? extends CustomPacketPayload> type(){return channel.payloadType;}
    }
    private static final class Registration<T> {
        final int id; final Class<T> type; final BiConsumer<T,FriendlyByteBuf> encoder; final Function<FriendlyByteBuf,T> decoder; final BiConsumer<T,Supplier<NetworkEvent.Context>> handler;
        Registration(int id,Class<T> type,BiConsumer<T,FriendlyByteBuf> encoder,Function<FriendlyByteBuf,T> decoder,BiConsumer<T,Supplier<NetworkEvent.Context>> handler){this.id=id;this.type=type;this.encoder=encoder;this.decoder=decoder;this.handler=handler;}
        void encode(Object message,FriendlyByteBuf buffer){encoder.accept(type.cast(message),buffer);} T decode(FriendlyByteBuf buffer){return decoder.apply(buffer);} void handle(Object message,NetworkEvent.Context context){handler.accept(type.cast(message),()->context);}
    }
}
''')
write("net/mcreator/scpadditions/fabric/FabricServerContext.java", r'''
package net.mcreator.scpadditions.fabric;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
public final class FabricServerContext {
    private static volatile MinecraftServer server;
    private FabricServerContext() {}
    public static void set(MinecraftServer value) { server=value; net.neoforged.neoforge.server.ServerLifecycleHooks.setCurrentServer(value); }
    public static Collection<ServerPlayer> players() { return server == null ? List.of() : server.getPlayerList().getPlayers(); }
}
''')

# Common Fabric event bridge. It posts the Neo-style wrappers consumed by the
# existing gameplay subscribers, preserving cancellation where Fabric exposes it.
write("net/mcreator/scpadditions/fabric/FabricGameEventBridge.java", r'''
package net.mcreator.scpadditions.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.*;

final class FabricGameEventBridge {
    private FabricGameEventBridge() {}
    static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(FabricServerContext::set);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FabricServerContext.set(null));
        ServerTickEvents.START_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(server)));
        ServerTickEvents.START_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(level)));
        ServerTickEvents.END_WORLD_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(level)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer)));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player)));
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, entity)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher)));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {});
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            PlayerInteractEvent.RightClickBlock event=new PlayerInteractEvent.RightClickBlock(player,hand,hit);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?event.getCancellationResult():InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            PlayerInteractEvent.RightClickItem event=new PlayerInteractEvent.RightClickItem(player,hand);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand)):net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            PlayerInteractEvent.EntityInteract event=new PlayerInteractEvent.EntityInteract(player,entity);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled()?event.getCancellationResult():InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            LivingIncomingDamageEvent event=new LivingIncomingDamageEvent(entity,source,amount);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getAmount() > 0;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity,source)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity,source)));
    }
}
''')

# Register common payload types after every legacy message has been declared.
def entrypoint_common(source: str) -> str:
    if "SimpleChannel.registerAllCommon();" not in source:
        source = source.replace("        FabricGameEventBridge.register();\n", "        FabricGameEventBridge.register();\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllCommon();\n")
    return source
rewrite("net/mcreator/scpadditions/fabric/ScpAdditionsFabric.java", entrypoint_common)

def entrypoint_client(source: str) -> str:
    if "SimpleChannel.registerAllClient();" not in source:
        source = source.replace("        FabricClientEventBridge.register();\n", "        FabricClientEventBridge.register();\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllClient();\n")
    return source
rewrite("net/mcreator/scpadditions/fabric/ScpAdditionsFabricClient.java", entrypoint_client)

print("Applied Fabric API round 2: persistent attachments, item handlers, network bridge, and server event surface")
