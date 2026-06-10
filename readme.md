# What is this mod?
This is an unofficial port of ReplayMod for Minecraft 1.20.1 Forge.

Please do not report issues from this version to the official ReplayMod support channels.

# Compatibility
Replays created with similar unofficial builds can usually be loaded normally.

# What Fix?
This port focuses on making the replay, recording, editing, and rendering pipeline work correctly on Forge 1.20.1.

Fixed and improved areas:

- Replaced Fabric/Yarn-oriented setup with Forge-friendly mod loading and mixin wiring.
- Cleaned up mixin class names and package handling to match the new `replayneo` namespace.
- Moved mod id, log text, and base mod identity to `replayneo`.
- Split client-only startup from dedicated-server construction so the mod no longer loads `Minecraft` client classes on a dedicated server.
- Fixed multiplayer-server recording startup. Recording now retries when early login packets arrive before server metadata is available, and it falls back to the current server or channel address when needed.
- Fixed the per-server automatic recording extension on `ServerData`, preventing `AbstractMethodError` during multiplayer recording startup.
- Fixed keybind registration, replay-only key conflict handling, and tick callback wiring so replay shortcuts, camera controls, recording events, and player overview controls do not trigger each other incorrectly.
- Fixed GUI mouse coordinate scaling and early layout initialization so buttons, replay timelines, and popup controls can be clicked reliably in both dev and packaged builds.
- Fixed packaged-build replay editor rendering, including invisible-but-clickable replay controls.
- Fixed recording controls and overlay visibility so recording buttons only appear on the intended pause/world-selection screens.
- Fixed local-player recording so singleplayer replays include the player's spawn, PlayerInfo, movement, rotation, equipment, riding updates, and effects left by movement.
- Fixed replay-side PlayerInfo fallback handling so recorded player entities can spawn when older/incomplete recordings are opened.
- Fixed replay player movement playback, including cases where movement packets arrived before the target player entity was fully present.
- Fixed Forge HUD and replay overlay rendering in packaged builds.
- Fixed replay HUD handling so Replay GUI and in-game GUI do not leak into exported video frames.
- Fixed replay camera and spectating behavior, including cross-dimension and fallback camera handling.
- Fixed local-player and replay entity visibility paths that could hide players or other entities incorrectly.
- Fixed chunk rebuild, dirty-section handling, and world-border timing hooks that could break world rendering updates.
- Fixed video rendering paths where forced chunk loading could crash or prevent blocks from being rendered.
- Fixed modded entity, player, and block rendering paths used during replay playback and video export.
- Fixed block update tracking so chunk dirtied updates are not silently lost before the view area exists.
- Fixed packet handling and diagnostics for local connections, login disconnects, replay packet failures, and replay recording shutdown.
- Fixed replay post-processing failures so unprocessed recordings are kept instead of crashing or losing the recording.
- Fixed resource-pack recording and replay resource loading paths.
- Fixed bundled GUI texture paths and missing `jgui:gui.png` resource lookup issues.
- Fixed replay editor, render-setting, popup, timeline, scrollbar, and recording-complete dialogs using the wrong GUI texture atlas.
- Converted language files from `.lang` to `.json`.
- Added and adjusted render settings behavior, including render method switching and blend export support.
- Started reorganizing the codebase into API, core, platform, and mixin boundaries so Minecraft/Forge-specific code is isolated from replay core logic.