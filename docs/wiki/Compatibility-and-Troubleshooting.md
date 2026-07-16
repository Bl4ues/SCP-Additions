# Compatibility and Troubleshooting

## Updating the mod

- Back up important worlds and configuration directories first.
- Existing published `scp_additions` registry IDs are retained where required for old-world compatibility.
- Intermediate animation blocks remain registered so old saves resolve them, but they are hidden from creative tabs.
- Existing customized configuration files are never automatically replaced.

## Missing optional mod content

Configuration entries may reference optional mods such as `scpo`. If an item, entity, effect, sound, or particle ID is unavailable, the affected entry is skipped and reported while unrelated entries remain active.

Malformed resource IDs are different: they invalidate the reload and keep the previous active configuration.

## Regenerating defaults

Close the game/server, back up the relevant JSON and `.bak`, delete only the file to regenerate, and start the installed version again.

Do not delete `914recipes.d`, Codex world assets, or the entire configuration directory unless those customizations are disposable.

## Codex image or text does not appear

1. Reopen the document editor and confirm `World image attached` or `world text attached`.
2. Press **Save Document** or **Save & Give Test Item**.
3. Confirm the document remains in the configuration list after reopening it.
4. Check the world's `scp_additions/codex_assets` directory.
5. Confirm the image is PNG/JPG/JPEG, at most 2.5 MB, and no larger than 4096×4096.

Unique generated documents must be created from the Codex editor. Do not assign `CODEX` through a generic item rule.

## SCP-914 skins do not render in multiplayer

The server synchronizes only the filename. Every relevant client must have the same PNG filename in `config/scpadditions/scp914_skins` and install Kleiders Custom Renderer API.

## Configuration save fails on Windows

The mod uses backup creation, atomic replacement where available, retries, and a safe fallback. If a save still fails:

- confirm the file is not read-only;
- close external editors holding the file;
- verify write permission for the instance directory;
- preserve the `.bak` before manual recovery.

## Reporting a bug

Use the [GitHub issue tracker](https://github.com/Bl4ues/SCP-Additions/issues) and include:

- SCP Additions version;
- Forge and GeckoLib versions;
- complete mod list;
- exact reproduction steps;
- `latest.log` and crash report when applicable;
- screenshots for visual or interface problems.
