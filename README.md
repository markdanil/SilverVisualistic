# SilverVisualistic 2.0.0

Minecraft Java Edition **1.21.4** · Fabric · Java **21**.

This source project continues the existing SilverVisualistic codebase and integrates SilverVisible account authentication and HitFeedback. The optional image-based menu theme is deliberately postponed; the default menu keeps the dark graphite / violet SilverVisualistic look with a moving header accent and hover feedback.

## SilverVisible account and online directory

- Settings are gated by SilverVisible authentication. Opening the settings through RIGHT_SHIFT or Mod Menu first opens the login screen if no valid account session exists.
- Register on `https://silvervisible-api.silvervisible.workers.dev/account`, link the Minecraft nickname there, then use the same SilverVisible login and password in the mod.
- The mod stores remembered account sessions locally, but never stores the account password. Multiple profiles can be saved and switched without logging out the other profile.
- The website session and the in-game mod session are independent. Each can be logged out separately.
- In-game presence is published automatically while a world is active: linked nickname, mod version, and current server address. The current server address is publicly visible in the SilverVisible directory, as selected by the project owner.
- Presence heartbeat: every 30 seconds. Player directory refresh: every 20 seconds. The server expires stale presence after 90 seconds. A best-effort goodbye is sent when leaving a world or changing accounts.
- Only stable mod versions `2.0.0` or newer are accepted by the SilverVisible service.
- The online list displays nickname, version, server address, and account role. Author badges come from the authenticated SilverVisible account role, not from the nickname alone.
- API endpoint: `https://silvervisible-api.silvervisible.workers.dev`.

**Identity notice:** a SilverVisible account proves control of that SilverVisible login, not ownership of a licensed Minecraft account. Minecraft nicknames are self-declared by the account holder. The service does not verify Mojang/Microsoft identity.

## Built-in HitFeedback

HitFeedback is integrated into SilverVisualistic. Do **not** install the standalone HitFeedback JAR alongside this mod.

- Normal hit: configurable HEX color (default `#39E65C`).
- Confirmed Minecraft critical hit: configurable HEX color (default `#FF3838`).
- Duration and tint strength are configurable.
- Only a player hit by the local player is tinted. Other players fighting each other are not tinted by this client.

## FullBright and target marker

- FullBright overrides both vanilla 1.21.4 `LightmapTextureManager.getBrightness` overloads and the night-vision blend while enabled.
- The target marker is a fixed square face facing the camera; the face geometry does not spin with the gradient animation.
- Sidebar categories are wheel-scrollable immediately without first selecting a category. The category list is capped to eight visible rows and preserves its scroll position while scrolling.

## Gameplay and menu

- New **Игровой процесс** section contains Streamer Mode: local HUD display of the current nickname and server address, each independently toggleable.
- SilverVisible presence is independent from the Streamer Mode HUD display. Hiding the HUD does not stop the required public presence while signed in and in a world.
- The menu uses a dark violet visual style, animated header sweep, and hover feedback. The optional custom image theme is not included in this build yet.

## Build

Use Java 21 and Gradle. From the project root, run:

```powershell
gradle build
```

The remapped mod JAR is written to `build/libs/`.

Dependencies: Fabric Loader, Fabric API, Mod Menu 13.0.4. No SoupAPI, SoupVisuals, or standalone HitFeedback dependency is required.


### Source revision 3

Includes a networking fix for SilverVisible login. This is a source update for testing, not a published official release.
