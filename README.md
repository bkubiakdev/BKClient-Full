<p align="center">
  <img src="https://i.imgur.com/6LCMpug.png" alt="BKClient" width="280">
</p>

<h1 align="center">BKClient Full</h1>

<p align="center">
  Minecraft 1.21.4 · Fabric · Java 21 · Client 3.0.8
</p>

<p align="center">
  <a href="https://bkubiak.dev/mods">Website</a> ·
  <a href="https://dc.bkubiak.dev">Discord</a> ·
  <a href="https://donate.bkubiak.dev">Support the project</a>
</p>

BKClient Full is a Minecraft client modification with combat and movement modules, configurable visuals, an editable HUD, market automation and music tools. This repository contains the Full edition for Minecraft **1.21.4**.

## Features

| Area | Included features |
| --- | --- |
| Interface | ClickGUI, classic and Spotlight menus, module search, themes, keybinds, custom chat and HUD customization. |
| Combat | Aura, Aim Assist, TriggerBot, Auto Totem, Auto Armor, Auto Potion, Auto Crystal, Auto Mace, Anchor Exploder, BackTrack and WTap. |
| Movement | Auto Sprint, Speed, Strafe, Flight, WindHop, No Slow, No Web, Spider and Elytra utilities. |
| Inventory and building | Inventory Cleaner, Inventory Set, Auto Tool, Auto Swap, Stealer, Scaffold, Fast Place and MineHelper. |
| Visuals | ESP, Storage ESP, Target ESP, tracers, projectile prediction, custom crosshair, hotbar, sword models, swing animations and ViewModel. |
| World and camera | Ambience, sky and fog effects, Blizzard, particles, trails, jump circles, FreeCam, Free Look, Zoom and Smooth Camera. |
| Personalization | Capes, skins, cosmetics, emotes and pets. |
| Market tools | AutoMarket profiles, item filters, price scanning, purchase and sale history, auto selling and configurable Discord webhook notifications. |
| Music | Media information, album artwork, synced lyrics, a lyrics editor and Lyrics 3D. |
| Utilities | Friends, waypoints, death coordinates, local configuration profiles, TinyTask and Windows window capture. |

Server-specific modules depend on the server's menus, item formats and rules. Windows media integration, native video playback and window capture use Windows-specific components.

## Getting started

Use **Java 21**, Minecraft **1.21.4**, Fabric Loader **0.16.14** and Fabric API **0.110.5+1.21.4**, matching `gradle.properties`.

1. Build the client using the commands below.
2. Put `build/libs/BK-Client-Full-3.0.8.jar` and the matching Fabric API JAR in your Minecraft instance's `mods` directory.
3. Launch the Fabric profile and press **Right Shift** in game to open the menu.
4. Use `.help` in chat to list client commands. The default command prefix is `.`.

Local data is stored in the instance's `BK-Client` directory. A development launch uses `run/BK-Client`.

### Build

Set `JAVA_HOME` to your Java 21 installation. The Gradle wrapper is included; the first build needs internet access to download dependencies and Minecraft assets.

Windows PowerShell:

```powershell
.\gradlew.bat remapJar
```

Linux / macOS shell:

```sh
sh ./gradlew remapJar
```

The packaged client is written to `build/libs/`. The bundled native components target Windows x64; the shell command alone does not imply full platform support.

### Run from source

```powershell
.\gradlew.bat runClient
```

On Linux or macOS, use `sh ./gradlew runClient`, subject to the native-component limitations above. Development files and logs are created under `run/`.

## Online services and startup behavior

This source snapshot uses **`https://changeme.net`** placeholders for cloud configuration sharing, friend presence, remote AutoMarket control, AirDrop and version checks. The server implementation is not included. Those features need your own compatible backend and real endpoint URLs. AirDrop and friend popups are currently disabled in `DeferredFeatures`.

Endpoint definitions are in:

- `utility/game/XSK.java` — cloud configuration URLs.
- `utility/game/ClientTelemetryUtility.java` — friend presence.
- `utility/game/AutoMarketRemote.java` — remote market control.
- `systems/supabase/SupabaseService.java` — AirDrop.
- `systems/update/UpdateChecker.java` — version checks.

These paths are relative to `src/main/java/dev/bkclient/`. Search for `changeme.net` to find the corresponding website links and metadata too. Placeholder URLs are still used by the request code; they are not a working service or an offline switch.

On each launch, the client opens the [website](https://bkubiak.dev/mods) and [Discord redirect](https://dc.bkubiak.dev) in your default browser. After joining a world, it shows a clickable donation message locally in chat once per game session. This behavior is implemented in `utility/game/SupportNotice.java`.

Lyrics use LRCLIB and NetEase, with Musixmatch as a timed-lyrics fallback. Genius supplies optional plain lyrics when `BKCLIENT_GENIUS_TOKEN` is set in your environment. No Genius token is included. Artwork and other integrations contact their respective external services.

## Source layout

```text
src/main/java/dev/bkclient/
  systems/        Modules, settings, commands, profiles and integrations
  ui/             Menus, HUD and editors
  mixin/          Minecraft and compatibility hooks
  utility/        Rendering, audio, networking and shared helpers
  combatengine/   Combat logic and rotation support
src/main/resources/
  assets/         Textures, shaders, models, sounds and translations
  licenses/       Attribution and bundled license notices
  third_party/    Upstream license and provenance files
```

## Issues

Report bugs through this repository's **Issues** tab. Include the client version, Minecraft and Fabric versions, steps to reproduce, and the relevant log excerpt. Remove account details, webhook URLs and tokens before posting logs.

## License and credits

BKClient contributions are covered by the [MIT License](LICENSE), copyright **2026 bkubiakdev**. Keep the copyright and license notice in copies and redistributed versions.

Imported code, assets and dependencies retain their original terms. See [third-party attribution](src/main/resources/licenses/ATTRIBUTION.txt) and the [world visuals notices](src/main/resources/third_party/world-visuals/NOTICE.txt).

## Support

If you use this source code and want to support its development, you can [donate here](https://donate.bkubiak.dev).
