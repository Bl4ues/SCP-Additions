# SCP Additions 3.0.7 Multiplayer Hotfix Validation

This marker triggers validation of the fully materialized Forge 1.20.1 hotfix source.

Required gates:

- regression-surface validator;
- Forge build and compiled JAR;
- asset audit;
- dedicated-server startup;
- graphical-client startup.

The runtime source must remain unchanged by the materializers during this validation run.
