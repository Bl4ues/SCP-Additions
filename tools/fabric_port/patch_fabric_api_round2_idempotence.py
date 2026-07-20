from __future__ import annotations

from pathlib import Path

root = Path(__file__).resolve().parents[2]
path = root / "tools/fabric_port/apply_fabric_api_round2.py"
source = path.read_text(encoding="utf-8")

source = source.replace(
    "def inventory_attachment(source: str) -> str:\n    start = source.index(\"    public static final DeferredRegister<AttachmentType<?>> REGISTRY\")",
    "def inventory_attachment(source: str) -> str:\n    if \"AttachmentRegistry.create(\" in source:\n        return source\n    start = source.index(\"    public static final DeferredRegister<AttachmentType<?>> REGISTRY\")",
)
source = source.replace(
    "def variables_attachment(source: str) -> str:\n    start = source.index(\"\\tpublic static final DeferredRegister<AttachmentType<?>> ATTACHMENTS\")",
    "def variables_attachment(source: str) -> str:\n    if \"AttachmentRegistry.create(\" in source:\n        return source\n    start = source.index(\"\\tpublic static final DeferredRegister<AttachmentType<?>> ATTACHMENTS\")",
)
source = source.replace(
    "def entrypoint_common(source: str) -> str:\n    source = source.replace(\"        FabricGameEventBridge.register();\\n\", \"        FabricGameEventBridge.register();\\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllCommon();\\n\")",
    "def entrypoint_common(source: str) -> str:\n    if \"SimpleChannel.registerAllCommon();\" not in source:\n        source = source.replace(\"        FabricGameEventBridge.register();\\n\", \"        FabricGameEventBridge.register();\\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllCommon();\\n\")",
)
source = source.replace(
    "def entrypoint_client(source: str) -> str:\n    source = source.replace(\"        FabricClientEventBridge.register();\\n\", \"        FabricClientEventBridge.register();\\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllClient();\\n\")",
    "def entrypoint_client(source: str) -> str:\n    if \"SimpleChannel.registerAllClient();\" not in source:\n        source = source.replace(\"        FabricClientEventBridge.register();\\n\", \"        FabricClientEventBridge.register();\\n        com.bl4ues.scpadditions.compat.network.SimpleChannel.registerAllClient();\\n\")",
)

path.write_text(source, encoding="utf-8")
print("Fabric API round 2 migration is idempotent")
