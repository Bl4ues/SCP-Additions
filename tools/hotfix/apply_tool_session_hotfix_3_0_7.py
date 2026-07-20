#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed: list[str] = []


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    old = path.read_text(encoding="utf-8")
    if old != text:
        path.write_text(text, encoding="utf-8")
        changed.append(rel)


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"Expected source fragment not found in {rel}: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))


# Mining speed is predicted client-side. A usable item therefore must remain in
# the client's selected hotbar slot for the whole authoritative usable session.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/UsableHotbarSessionClient.java",
    '''        ItemStack active = player.getInventory().items.get(activeSlot);
        if (!active.isEmpty() && isSameSingleItem(active, activeStack)) {
            return;
        }

        ItemStack copy = activeStack.copy();
''',
    '''        ItemStack active = player.getInventory().items.get(activeSlot);
        if (!active.isEmpty()) {
            if (isSameMutableUsableItem(active, activeStack)) {
                activeStack = normalizedSingle(active);
            }
            return;
        }

        ItemStack copy = activeStack.copy();
''',
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/UsableHotbarSessionClient.java",
    '''    private static boolean shouldClientReapplySessionCopy(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == UseAnim.SPYGLASS;
    }
''',
    '''    private static boolean shouldClientReapplySessionCopy(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }
''',
)

# WEAPON equipment creates a vanilla hotbar mirror. The slot contents were sent
# to clients, but the selected-slot index was not, leaving the client to mine as
# though it still held the previous item or an empty hand.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryActionPacket.java",
    "import net.minecraft.network.FriendlyByteBuf;\n",
    "import net.minecraft.network.FriendlyByteBuf;\n"
    "import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;\n",
)
replace_once(
    "src/main/java/com/bl4ues/scpinventory/network/InventoryActionPacket.java",
    '''        inventory.setItem(mirrorSlot, copy);
        if (mirrorSlot < VANILLA_HOTBAR_END_EXCLUSIVE) inventory.selected = mirrorSlot;
        ScpPickupRouter.syncVanillaInventory(player);
''',
    '''        inventory.setItem(mirrorSlot, copy);
        if (mirrorSlot < VANILLA_HOTBAR_END_EXCLUSIVE) {
            inventory.selected = mirrorSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(mirrorSlot));
        }
        ScpPickupRouter.syncVanillaInventory(player);
''',
)

print(f"Usable and weapon tool hotfix changed {len(changed)} files")
for rel in changed:
    print(rel)
