# Fabric 1.21.1 Port — SCP Additions 3.0.7

Branch: `fabric-1.21.1-port-3.0.7`

This branch is the independent Fabric 1.21.1 port of SCP Additions 3.0.7.

It remains separate from `master`, which continues to be the Forge 1.20.1 edition, and separate from `neoforge-1.21.1-port-3.0.7`. No loader-specific implementation is merged into master.

## Validation

The branch has its own Fabric Loom build, Fabric metadata, generated loader bridge, compiled-JAR artifact, and dedicated-server smoke test. Shared Minecraft 1.21.1 source migrations are imported before the Fabric-specific bridge is generated; loader registration, events, networking, attachments, capabilities, rendering hooks, and client setup remain Fabric-owned implementations.

The Fabric edition is not complete until it builds, launches on both client and dedicated server, preserves world/data compatibility where technically possible, and passes a full feature-parity checklist covering every SCP, inventory/crafting/status/codex workflow, configuration system, facility block, animated door, reader, survival mechanic, sound, overlay, networking path, and optional integration.
