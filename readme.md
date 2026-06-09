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
- Fixed keybind registration, replay-only key conflict handling, and tick callback wiring so replay shortcuts, camera controls, recording events, and player overview controls do not trigger each other incorrectly.
- Fixed GUI mouse coordinate scaling and early layout initialization so buttons, replay timelines, and popup controls can be clicked reliably in both dev and packaged builds.
- Fixed recording controls and overlay visibility so recording buttons only appear on the intended screens.
- Fixed local-player recording so singleplayer replays include the player's spawn, PlayerInfo, movement, rotation, equipment, and riding updates.
- Fixed replay-side PlayerInfo fallback handling so recorded player entities can spawn when older/incomplete recordings are opened.
- Fixed Forge HUD and replay overlay rendering in packaged builds, including cases where the input screen existed but replay buttons were not drawn.
- Fixed replay HUD handling so Replay GUI and in-game GUI do not leak into exported video frames.
- Fixed replay camera and spectating behavior, including cross-dimension and fallback camera handling.
- Fixed local-player and replay entity visibility paths that could hide players or other entities incorrectly.
- Fixed chunk rebuild, dirty-section handling, and world-border timing hooks that could break world rendering updates.
- Fixed video rendering paths where forced chunk loading could prevent blocks from being rendered.
- Fixed modded entity, player, and block rendering paths used during replay playback and video export.
- Fixed block update tracking so chunk dirtied updates are not silently lost before the view area exists.
- Fixed packet handling and diagnostics for local connections, login disconnects, and replay recording shutdown.
- Fixed replay post-processing failures so unprocessed recordings are kept instead of crashing or losing the recording.
- Fixed resource-pack recording and replay resource loading paths.
- Fixed bundled GUI texture paths and missing `jgui:gui.png` resource lookup issues.
- Converted language files from `.lang` to `.json`.
- Added and adjusted render settings behavior, including render method switching and blend export support.
