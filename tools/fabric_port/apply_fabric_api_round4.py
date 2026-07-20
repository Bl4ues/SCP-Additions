from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"
changed: list[str] = []


def write(path: Path, text: str) -> None:
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != text:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        changed.append(str(path.relative_to(ROOT)))


def edit(path: Path, fn) -> None:
    old = path.read_text(encoding="utf-8")
    new = fn(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(str(path.relative_to(ROOT)))


def remove_method(text: str, signature_fragment: str) -> str:
    while True:
        sig = text.find(signature_fragment)
        if sig < 0:
            return text
        start = text.rfind("@Override", 0, sig)
        if start < 0 or sig - start > 100:
            start = sig
        brace = text.find("{", sig)
        depth = 0
        end = None
        for i in range(brace, len(text)):
            if text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
                if depth == 0:
                    end = i + 1
                    break
        if end is None:
            raise RuntimeError(f"Unclosed method for {signature_fragment}")
        while end < len(text) and text[end] in " \t":
            end += 1
        if end < len(text) and text[end] == "\r":
            end += 1
        if end < len(text) and text[end] == "\n":
            end += 1
        text = text[:start] + text[end:]


color = JAVA / "net/neoforged/neoforge/client/event/RegisterColorHandlersEvent.java"
edit(color, lambda t: t.replace("import net.minecraft.world.item.ItemLike;", "import net.minecraft.world.level.ItemLike;"))

facility = JAVA / "net/mcreator/scpadditions/facility/FacilityModule.java"
def patch_facility(t: str) -> str:
    if "import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;" not in t:
        anchor = "package net.mcreator.scpadditions.facility;\n"
        t = t.replace(anchor, anchor + "\nimport net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;\n")
    return t.replace("CreativeModeTab.builder()", "FabricItemGroup.builder()")
edit(facility, patch_facility)

clone_re = re.compile(
    r"public ItemStack getCloneItemStack\(BlockState (?:state|blockState), HitResult target, "
    r"LevelReader (world|level), BlockPos pos, Player player\)"
)
for path in JAVA.rglob("*.java"):
    def patch_clone(t: str) -> str:
        def repl(m: re.Match[str]) -> str:
            state_name = "blockState" if "BlockState blockState" in m.group(0) else "state"
            return f"public ItemStack getCloneItemStack(LevelReader {m.group(1)}, BlockPos pos, BlockState {state_name})"
        return clone_re.sub(repl, t)
    edit(path, patch_clone)

for path in JAVA.rglob("*.java"):
    if "canHarvestBlock(" in path.read_text(encoding="utf-8"):
        def patch_harvest(t: str) -> str:
            if "requiresCorrectToolForDrops()" not in t:
                raise RuntimeError(f"Refusing to remove harvesting hook without vanilla tool requirement: {path}")
            return remove_method(t, "public boolean canHarvestBlock(")
        edit(path, patch_harvest)

for path in JAVA.rglob("*.java"):
    if "canConnectRedstone(" not in path.read_text(encoding="utf-8"):
        continue
    def patch_redstone(t: str) -> str:
        had_source = "boolean isSignalSource(BlockState state)" in t
        t = remove_method(t, "public boolean canConnectRedstone(")
        if not had_source:
            marker = "\n\t@Override\n\tpublic List<ItemStack> getDrops("
            method = "\n\t@Override\n\tpublic boolean isSignalSource(BlockState state) {\n\t\treturn true;\n\t}\n"
            if marker not in t:
                raise RuntimeError(f"No insertion point for Fabric redstone source: {path}")
            t = t.replace(marker, method + marker, 1)
        return t
    edit(path, patch_redstone)

for path in JAVA.rglob("*.java"):
    if "onDestroyedByPlayer(" not in path.read_text(encoding="utf-8"):
        continue
    def patch_destroy(t: str) -> str:
        pattern = re.compile(
            r"@Override\s+public boolean onDestroyedByPlayer\(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid\) \{\s+"
            r"boolean retval = super\.onDestroyedByPlayer\(blockstate, world, pos, entity, willHarvest, fluid\);\s+"
            r"([^;]+;)\s+return retval;\s+\}"
        )
        def repl(m: re.Match[str]) -> str:
            action = m.group(1)
            return ("@Override\n\tpublic BlockState playerWillDestroy(Level world, BlockPos pos, BlockState blockstate, Player entity) {\n"
                    "\t\tBlockState result = super.playerWillDestroy(world, pos, blockstate, entity);\n"
                    f"\t\t{action}\n\t\treturn result;\n\t}}")
        new, count = pattern.subn(repl, t)
        if count != 1:
            raise RuntimeError(f"Unexpected destruction hook shape: {path}")
        return new
    edit(path, patch_destroy)

for rel in [
    "net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
    "net/mcreator/scpadditions/procedures/Scp294drinkGiveProcedure.java",
    "net/mcreator/scpadditions/procedures/Scp294restockProcedure.java",
]:
    path = JAVA / rel
    def patch_close(t: str) -> str:
        if "import net.minecraft.server.level.ServerPlayer;" not in t:
            t = re.sub(r"(package [^;]+;\n)", r"\1\nimport net.minecraft.server.level.ServerPlayer;\n", t, count=1)
        return re.sub(
            r"(?m)^(\s*)(player|_player)\.closeContainer\(\);",
            r"\1if (\2 instanceof ServerPlayer serverPlayer) serverPlayer.closeContainer();",
            t,
        )
    edit(path, patch_close)

write(JAVA / "net/mcreator/scpadditions/fabric/menu/LegacyMenuData.java", '''package net.mcreator.scpadditions.fabric.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Consumer;

public record LegacyMenuData(byte[] payload) {
    private static final int MAX_PAYLOAD_SIZE = 1 << 20;

    public static final StreamCodec<RegistryFriendlyByteBuf, LegacyMenuData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeByteArray(data.payload()),
            buffer -> new LegacyMenuData(buffer.readByteArray(MAX_PAYLOAD_SIZE)));

    public static LegacyMenuData create(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writer.accept(buffer);
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), payload);
        buffer.release();
        return new LegacyMenuData(payload);
    }

    public FriendlyByteBuf toBuffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
    }
}
''')

write(JAVA / "net/mcreator/scpadditions/fabric/menu/LegacyMenuProvider.java", '''package net.mcreator.scpadditions.fabric.menu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;
import java.util.function.Supplier;

public final class LegacyMenuProvider implements ExtendedScreenHandlerFactory<LegacyMenuData> {
    @FunctionalInterface
    public interface Factory {
        AbstractContainerMenu create(int id, Inventory inventory, Player player, LegacyMenuData data);
    }

    private final Component title;
    private final Factory factory;
    private final Supplier<LegacyMenuData> openingData;

    public LegacyMenuProvider(Component title, Factory factory, Supplier<LegacyMenuData> openingData) {
        this.title = Objects.requireNonNull(title);
        this.factory = Objects.requireNonNull(factory);
        this.openingData = Objects.requireNonNull(openingData);
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return factory.create(id, inventory, player, openingData.get());
    }

    @Override
    public LegacyMenuData getScreenOpeningData(ServerPlayer player) {
        return openingData.get();
    }
}
''')

write(JAVA / "net/neoforged/neoforge/common/extensions/IMenuTypeExtension.java", '''package net.neoforged.neoforge.common.extensions;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.mcreator.scpadditions.fabric.menu.LegacyMenuData;

public final class IMenuTypeExtension {
    private IMenuTypeExtension() {}

    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory, FriendlyByteBuf data);
    }

    public static <T extends AbstractContainerMenu> MenuType<T> create(Factory<T> factory) {
        return new ExtendedScreenHandlerType<>(
                (syncId, inventory, data) -> factory.create(syncId, inventory, data.toBuffer()),
                LegacyMenuData.STREAM_CODEC);
    }
}
''')

dial_files = [
    "Scp914dial1to1Block.java", "Scp914dialRoughBlock.java", "Scp914dialCoarseBlock.java",
    "Scp914dialFineBlock.java", "Scp914dialVeryFineBlock.java",
]
for name in dial_files:
    path = JAVA / "net/mcreator/scpadditions/block" / name
    def patch_dial_menu(t: str) -> str:
        if "LegacyMenuProvider" not in t:
            t = re.sub(r"(package [^;]+;\n)", r"\1\nimport net.mcreator.scpadditions.fabric.menu.LegacyMenuData;\nimport net.mcreator.scpadditions.fabric.menu.LegacyMenuProvider;\n", t, count=1)
        pattern = re.compile(r'''player\.openMenu\( new MenuProvider\(\) \{.*?\n\s*\}, pos\);''', re.S)
        replacement = '''player.openMenu(new LegacyMenuProvider(
                    Component.literal("SCP-914 dial"),
                    (id, inventory, menuPlayer, data) -> new Scp914GuiMenu(id, inventory, data.toBuffer()),
                    () -> LegacyMenuData.create(data -> data.writeBlockPos(pos))));'''
        new, count = pattern.subn(replacement, t)
        if count == 0 and "new LegacyMenuProvider(" not in t:
            raise RuntimeError(f"Could not convert dial menu: {path}")
        return new
    edit(path, patch_dial_menu)

terminal = JAVA / "net/mcreator/scpadditions/block/TeslaTerminalBlockBlock.java"
def patch_terminal_menu(t: str) -> str:
    if "LegacyMenuProvider" not in t:
        t = re.sub(r"(package [^;]+;\n)", r"\1\nimport net.mcreator.scpadditions.fabric.menu.LegacyMenuData;\nimport net.mcreator.scpadditions.fabric.menu.LegacyMenuProvider;\n", t, count=1)
    pattern = re.compile(r'''player\.openMenu\( new MenuProvider\(\) \{.*?\n\s*\}, data -> \{\s*data\.writeBlockPos\(pos\);\s*data\.writeBoolean\(teslaOn\);\s*data\.writeBoolean\(manualOverride\);\s*\}\);''', re.S)
    replacement = '''player.openMenu(new LegacyMenuProvider(
                    Component.literal("Tesla Terminal"),
                    (id, inventory, menuPlayer, data) -> new TeslaTerminalMenu(id, inventory, data.toBuffer()),
                    () -> LegacyMenuData.create(data -> {
                        data.writeBlockPos(pos);
                        data.writeBoolean(teslaOn);
                        data.writeBoolean(manualOverride);
                    })));'''
    new, count = pattern.subn(replacement, t)
    if count == 0 and "new LegacyMenuProvider(" not in t:
        raise RuntimeError("Could not convert Tesla terminal menu")
    return new
edit(terminal, patch_terminal_menu)

print(f"Fabric API round 4 changed {len(changed)} files")
for item in changed:
    print(item)
