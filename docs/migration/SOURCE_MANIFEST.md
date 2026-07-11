# Migration source manifest

## SCP Additions host

- Repository: `Bl4ues/SCP-Additions`
- Integration branch: `integration/scp-additions-3.0`
- Base release line: SCP Additions 2.x

## SCP Inventory

- Repository: `Bl4ues/SCPInventory`
- Canonical export branch: `integration/additions-3.0-export`
- Standalone source commit used to generate the export: `94378719ec731f0ebaa6b72b7ff657a8a84c68b5`
- Canonical Java source path: `migration-export/java`

The exported Java source already includes the Gradle-applied layout, router, maintenance and usable-session fixes. Migration work must read from the export rather than copying the unpatched standalone `src/main/java` tree.

## SCP Unity Extra Blocks

- Repository: `Bl4ues/SCP-Unity-Extra-Blocks`
- Source namespace: `scp_unity_extra_blocks`
- Standalone compatibility is not required
- The source project remains the reference for block classes, procedures, registries and facility behavior

## Imported resources

The integration branch already contains resource trees from both source projects:

- `src/main/resources/assets/scpinventory`
- `src/main/resources/data/scpinventory`
- `src/main/resources/assets/scp_unity_extra_blocks`

These namespaces remain intact during the first implementation phases.
