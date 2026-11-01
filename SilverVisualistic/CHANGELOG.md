# Changelog

## SilverVisualistic 2.0.0 — integration revision

### Added
- SilverVisible website account login inside the mod settings gate.
- Locally remembered account profiles with account switching without deleting other saved profiles.
- Authenticated online presence, server-address publication, player directory, version and author role display.
- Built-in HitFeedback with independent normal/critical HEX colors, duration, and tint strength.
- Streamer Mode HUD with independently controlled nickname and current server address.
- Animated purple header accent and hover feedback in the settings menu.

### Fixed / improved
- Sidebar categories now respond to the mouse wheel immediately, without requiring a category click first; scroll position is preserved while scrolling.
- FullBright now overrides both vanilla `LightmapTextureManager.getBrightness` overloads in Minecraft 1.21.4, in addition to the night-vision blend hook.
- Marker geometry is a non-rotating square face oriented toward the camera, rather than animated corner brackets.
- HitFeedback renderer mixin is aligned with the standalone 1.21.4 source callback shape and is integrated directly into this mod.

### Deferred
- The optional image-based menu theme / Decorations theme is intentionally not included in this revision.
