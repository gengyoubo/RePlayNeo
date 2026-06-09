# Platform Boundary Refactor

The target architecture is:

- `api`: loader-neutral interfaces and data records only.
- `core`: replay, recording, editing, exporting, timeline, and camera logic.
- `platform`: Minecraft/Forge adapters that implement `api`.
- `mixin`: injection and accessors only; forward captured state into platform/core services.

## Rules

- `api` must not import `net.minecraft`, `net.minecraftforge`, or mixin classes.
- `core` should move toward the same rule. During migration this is reported but not enforced by default.
- `platform` is the only layer that should translate Minecraft/Forge objects into replay abstractions.
- `mixin` should stay thin: capture, adapt, forward.

## Suggested Migration Order

1. Input and key bindings.
2. HUD and render callbacks.
3. Network packet capture/playback.
4. Entity/world adapters.
5. Camera and video export adapters.

## Current Slice

- Added the loader-neutral `api` package for client, input, render, network, entity, world, and camera boundaries.
- Added a Forge platform entry point and moved key binding registration behind `ReplayInput` / `ReplayKeyBindingRegistry`.
- Moved runtime environment queries behind `ReplayRuntime`, including mod version, Minecraft version, game directory, and installed network mods.
- Moved config/replay folder path resolution and mouse coordinate lookup away from direct Minecraft access in `core`.
- Moved mixin launcher/config glue, replay restriction payload handling, replay button texture binding, text measurement, and client translation behind platform-facing classes.
- Added `verifyReplayLayering` so `api` fails on Minecraft/Forge/mixin imports, while `core` reports remaining migration work.
