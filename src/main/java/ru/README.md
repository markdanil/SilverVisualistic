# SilverVisualistic 2.0.0 — integrated SilverVisible and HitFeedback build

Minecraft Java Edition 1.21.4 / Fabric / Java 21.

This source tree continues the SilverVisualistic 2.0.0 line. It retains the pre-existing client features and adds required SilverVisible account integration, built-in HitFeedback, a fixed dark violet settings UI, a cube-face target marker, a lightmap FullBright hook, and the Streamer Mode HUD. The optional image-based menu Theme is deliberately postponed and is not included in this build.

## SilverVisible accounts and online directory

- The Mod Menu entry and `RIGHT_SHIFT` open an authentication gate. The actual settings screen is opened only after a successful SilverVisible login and a linked Minecraft nickname.
- Login uses the SilverVisible challenge + PBKDF2-HMAC-SHA256 verifier flow. The password is never stored in the local profile file; the client clears the password character buffer after login. The local profile file stores per-account bearer session tokens so that several accounts can be remembered and switched without signing the previous one out.
- The local file is `config/silvervisible-profiles.json`. It should not be shared publicly because it contains session tokens.
- After login, whenever the player is in a world, the mod publishes the linked nickname, mod version `2.0.0`, and the current server address to the public directory using a random per-world-session UUID. The server address is explicitly public in the SilverVisible listing, as requested for this project.
- Heartbeats are sent every 30 seconds; the API expires sessions after 90 seconds without a heartbeat. On a normal world disconnect the mod sends a best-effort goodbye request.
- The public listing is refreshed every 20 seconds and includes nickname, version, server address and account role. Author badges are based on the account role returned by SilverVisible, never on the nickname text.
- Users can switch between saved accounts without revoking the old account's token. The in-mod **Выйти** button revokes and removes the active mod session. The website browser session is separately managed on the website.
- API base URL: `https://silvervisible-api.silvervisible.workers.dev`.

**Identity notice:** SilverVisible verifies ownership of a SilverVisible account, not ownership of a Minecraft account. Minecraft nicknames are self-declared on the account page and are not verified against Mojang/Microsoft. Public server addresses are visible to anyone viewing the player directory. Do not treat nicknames or author badges as anti-impersonation proof outside the SilverVisible account system.

## Built-in HitFeedback

HitFeedback is integrated into SilverVisualistic; do **not** install a separate HitFeedback JAR. It marks only a player attacked by the local player. Normal and confirmed critical hits can use separate HEX colors, with configurable duration and tint strength. It does not mark players fighting each other unless the local client attacks them.

## FullBright and target marker

- FullBright hooks the Minecraft 1.21.4 lightmap brightness calculation while the setting is enabled.
- The marker style is a screen-facing square face, with a basis built from the camera direction. It does not spin with the gradient and remains square when looking up/down.

## Streamer Mode

The **Игровой процесс** settings section contains a local HUD panel for the current Minecraft nickname and current server address. Each line can be hidden independently. This HUD is local presentation; SilverVisible separately publishes the same server address when the account is logged in and the player is in a world.

## Menu

The menu uses a fixed dark graphite/purple palette inspired by the SilverVisible website, with subtle animated accent and hover feedback. The optional image-based **Theme** feature is not included yet. The category rail accepts the mouse wheel across the entire sidebar without rebuilding the screen on every wheel event.

## Build

Use Java 21 and Gradle. Run `gradle build`; output is in `build/libs/`.

Dependencies: Fabric Loader, Fabric API, Mod Menu 13.0.4. No SoupAPI / SoupVisuals / ZakoOpt dependency.
