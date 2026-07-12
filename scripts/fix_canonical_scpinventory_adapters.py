from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/bl4ues/scpinventory"
CANONICAL = ROOT / "migration-reference/scpinventory-final/java/com/bl4ues/scpinventory"


def replace(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected text not found in {path}: {old}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    replace(
        SRC / "network/ContextInteractPacket.java",
        "import com.bl4ues.scpinventory.entity.AbstractScp131Entity;",
        "import net.mcreator.scpadditions.entity.AbstractScp131Entity;",
    )
    replace(
        SRC / "client/ContextPromptClient.java",
        "import com.bl4ues.scpinventory.entity.AbstractScp131Entity;",
        "import net.mcreator.scpadditions.entity.AbstractScp131Entity;",
    )
    replace(
        SRC / "client/gui/components/StatusPanel.java",
        "import com.bl4ues.scpinventory.client.PlayerVitalsClient;",
        "import net.mcreator.scpadditions.vitals.client.PlayerVitalsClient;",
    )

    debug_source = CANONICAL / "debug/ScpInventoryDebug.java"
    debug_target = SRC / "debug/ScpInventoryDebug.java"
    debug_target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(debug_source, debug_target)

    print("Canonical integration adapters fixed")


if __name__ == "__main__":
    main()
