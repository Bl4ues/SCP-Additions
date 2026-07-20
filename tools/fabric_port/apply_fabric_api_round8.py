from __future__ import annotations

import json
import runpy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)


def edit(rel: str, transform) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8")
    new = transform(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(rel)


# The generated Fabric block entities expose vanilla WorldlyContainer access,
# so the NeoForge capability-registration callback is neither needed nor safe.
# SimpleEventBus cannot recover a generic Consumer's event type at runtime.
edit(
    "src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java",
    lambda text: text.replace("        bus.addListener(Scp294Capabilities::register);\n", ""),
)

write(
    "src/main/java/net/mcreator/scpadditions/fabric/mixin/client/SoundEngineMixin.java",
    '''package net.mcreator.scpadditions.fabric.mixin.client;

import java.util.Map;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Shadow @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(
            method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At("RETURN"))
    private void scpAdditions$registerStartedSound(
            SoundInstance sound,
            CallbackInfo callback) {
        ChannelAccess.ChannelHandle handle = instanceToChannel.get(sound);
        if (handle == null) return;

        SoundEngine engine = (SoundEngine) (Object) this;
        handle.execute(channel -> {
            Sound resolved = sound.getSound();
            if (resolved != null && resolved.shouldStream()) {
                NeoForge.EVENT_BUS.post(
                        new PlayStreamingSourceEvent(engine, channel, sound));
            } else {
                NeoForge.EVENT_BUS.post(
                        new PlaySoundSourceEvent(engine, channel, sound));
            }
        });
    }
}
''',
)

mixins_path = ROOT / "src/main/resources/scp_additions.mixins.json"
metadata = json.loads(mixins_path.read_text(encoding="utf-8"))
client = list(metadata.get("client", []))
name = "client.SoundEngineMixin"
if name not in client:
    client.append(name)
metadata["client"] = client
new_json = json.dumps(metadata, indent=2) + "\n"
old_json = mixins_path.read_text(encoding="utf-8")
if new_json != old_json:
    mixins_path.write_text(new_json, encoding="utf-8")
    changed.append("src/main/resources/scp_additions.mixins.json")

print(f"Fabric API round 8 changed {len(changed)} files")
for item in changed:
    print(item)

# Keep the existing workflow compatible while the migration frontier advances.
for script in (
    "apply_fabric_api_round9.py",
    "apply_fabric_api_round10.py",
    "apply_fabric_api_round11.py",
):
    runpy.run_path(
        str(ROOT / "tools/fabric_port" / script),
        run_name="__main__",
    )
