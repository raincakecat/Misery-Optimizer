<p align="center">
  <img src="fabric/src/client/resources/assets/tung/icon.png" width="128" alt="Misery Optimizer icon">
</p>

<h1 align="center">Misery Optimizer</h1>

<p align="center">
  <em><strong>A Fabric client mod for instant combat feedback in crystal PvP — tung tung tung 🥁</strong></em>
</p>

<p align="center">
  <a href="#-install-client"><img src="https://img.shields.io/badge/Minecraft-1.21.11-green" alt="Minecraft 1.21.11"></a>
  <a href="#-install-client"><img src="https://img.shields.io/badge/Java-21-blue" alt="Java 21"></a>
  <a href="#-install-client"><img src="https://img.shields.io/badge/requires-Fabric%20API-blueviolet" alt="Fabric API"></a>
  <a href="#-install-client"><img src="https://img.shields.io/badge/Mod%20ID-tung-orange" alt="Mod ID tung"></a>
  <img src="https://img.shields.io/badge/License-CC0--1.0-lightgrey" alt="CC0-1.0">
</p>

---

**Misery Optimizer** (mod id `tung`) is a client-side Fabric mod for **Minecraft 1.21.11** that makes crystal PvP feel immediate on high-latency connections. On your client it previews end crystal placement and break effects, charged respawn anchor explosions outside the Nether, ender-pearl flight, nearby explosion terrain, and the finish of the local eating animation — all before the server confirms anything. It also sends a pending hotbar slot before mining, and it is fully configurable from the in-game settings menu on key **O**. An optional **Paper plugin** sends exact explosion block lists and rejection notices so the client can correct its previews sooner.

### Honest scope

The mod does **not** reduce network latency or server tick time. The server stays authoritative: it still decides damage, knockback, terrain destruction, food effects, pearl teleportation, and whether an action ever succeeds. It does not change hit registration or PvP rules. What it gives you is *feedback* — the world responds instantly to *you*, then reconciles with what really happened.

## 📁 Repository layout

| Path | Contents |
| --- | --- |
| [`fabric/`](fabric/) | Fabric client mod source, Gradle wrapper, tests, and detailed feature documentation |
| [`paper/`](paper/) | Optional Paper plugin source, Gradle wrapper, and server configuration |
| [`jars/`](jars/) | Ready-to-install Minecraft 1.21.11 JARs |

## 🔧 Install (client)

1. Install **Fabric Loader 0.18.1 or newer** and **Fabric API** for Minecraft **1.21.11** and run **Java 21** or newer.
2. Drop this jar into the client's `mods` folder:

   | Project | Jar for your `mods` / `plugins` folder |
   | --- | --- |
   | Fabric client mod → `mods` | [`jars/MiseryOptimizer-Fabric-1.21.11-1.4.6.jar`](jars/MiseryOptimizer-Fabric-1.21.11-1.4.6.jar) |
   | Paper plugin (optional) → `plugins` | [`jars/MiseryOptimizer-Paper-1.21.11-1.1.0.jar`](jars/MiseryOptimizer-Paper-1.21.11-1.1.0.jar) |

3. Press **O** in game to open the settings menu: crystal break prediction, crystal placement preview, anchor feedback, ender-pearl projectile preview with adjustable server handoff (0–9 ticks), safe outline terrain preview with a maximum number of predicted blocks (128–768), early hotbar synchronization before mining, and local eating animation completion at the normal item duration.
4. Rebind the settings key under **Options → Controls → Key Binds → Misery Optimizer**. Pearl prediction and terrain preview are off by default. Turn **Terrain preview** on to see safe outlines; the older temporary-hole preview is available by turning **Safe outlines** off. Existing settings in `config/tung.json` keep their saved values.

See [`fabric/README.md`](fabric/README.md) for full behavior, configuration, and test details.

For a Modrinth upload, use the version settings, file hash, and publishing notes in [`MODRINTH_UPLOAD.md`](MODRINTH_UPLOAD.md).

## 🐟 Optional Paper plugin

For the tightest reconciliation, install [`MiseryOptimizer-Paper-1.21.11-1.1.0.jar`](jars/MiseryOptimizer-Paper-1.21.11-1.1.0.jar) on a Paper 1.21.11 server (Java 21) and restart. It sends exact server block lists for crystal and charged-anchor explosions (defaults: 2048 max blocks, 96-block range, 16 KiB per player per tick — see `plugins/MiseryOptimizerPaper/config.yml`) and rejection notices when another plugin cancels a crystal hit, anchor use, pearl throw, ordinary block placement/breaking, or food consumption. The client mod works fine without it.

Details: [`paper/README.md`](paper/README.md).

## ⚙️ Building

Run `./gradlew clean build` from `fabric/` or `paper/` with **JDK 21** or newer. The installable JAR appears in that project's `build/libs/`; the checked-in `jars/` are the release builds from these sources.

## 🏷️ Credits & license

Made by **raincakecat**.

Both projects are released under **CC0-1.0** — see their `LICENSE` files.

<p align="center"><sub>tung tung tung 🥁</sub></p>
