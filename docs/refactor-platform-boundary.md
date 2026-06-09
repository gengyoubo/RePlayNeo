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
