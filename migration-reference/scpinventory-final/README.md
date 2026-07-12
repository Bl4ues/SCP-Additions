# SCP Additions 3.0 migration export

This directory contains the final Java sources generated from the SCP Inventory build.

The export includes the results of:

- scpui-layout-overrides.gradle
- scpui-router-overrides.gradle
- scpui-maintenance-overrides.gradle
- scpui-usable-session-fixes.gradle

Generated with:

./gradlew clean generateScpUiLayoutSources
./gradlew build

Do not edit this directory as the standalone mod source.
It exists as the canonical migration reference for SCP Additions 3.0.
