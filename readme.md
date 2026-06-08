# What is this mod?
This is an unofficial port of ReplayMod for Minecraft 1.20.1 Forge.

Please do not report issues from this version to the official ReplayMod support channels.

# Compatibility
Replays created with similar unofficial builds can usually be loaded normally.

# What Fix?
This port focuses on making the replay, recording, and rendering pipeline work correctly on Forge 1.20.1.

Fixed and improved areas:

- Replaced Fabric/Yarn-oriented setup with Forge-friendly mod loading and mixin wiring.
- Cleaned up mixin class names and package handling to match the new `replayneo` namespace.
- Moved mod id, log text, and base mod identity to `replayneo`.
- Fixed keybind registration issues so replay shortcuts can be registered and triggered correctly.
- Fixed GUI mouse coordinate scaling so buttons and controls can be clicked reliably.
- Fixed recording controls and overlay hooks so the recording HUD can show and update correctly on Forge.
- Fixed replay HUD handling so the in-game GUI does not leak into exported video frames.
- Fixed replay camera and spectating behavior, including cross-dimension and fallback camera handling.
- Fixed local-player and replay entity visibility paths that could hide players or other entities incorrectly.
- Fixed chunk rebuild and world-border timing hooks that could break rendering updates.
- Fixed block update tracking so chunk dirtied updates are not silently lost before the view area exists.
- Fixed packet handling and diagnostics for local connections, login disconnects, and replay recording shutdown.
- Fixed resource-pack recording and replay resource loading paths.
- Converted language files from `.lang` to `.json`.
- Added and adjusted render settings behavior, including render method switching and blend export support.

Known work still pending:

- Iris/OptiFine specific compatibility is not covered here yet.

