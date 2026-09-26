# Misery Optimizer for Minecraft 1.21.11

Misery Optimizer is a Fabric client mod for faster local feedback during crystal PvP. An optional Paper plugin sends explosion block lists and rejection notices so the client can correct previews sooner. Both projects are kept in this repository.

## Layout

| Path | Contents |
| --- | --- |
| [`fabric/`](fabric/) | Fabric client mod source, Gradle wrapper, tests, and detailed feature documentation |
| [`paper/`](paper/) | Optional Paper plugin source, Gradle wrapper, and server configuration |
| [`jars/`](jars/) | Ready-to-install Minecraft 1.21.11 JARs |

## Install

1. Install Fabric Loader and Fabric API for Minecraft **1.21.11** on the client. Use Java 21 or newer.
2. Put [`MiseryOptimizer-Fabric-1.21.11-1.4.1.jar`](jars/MiseryOptimizer-Fabric-1.21.11-1.4.1.jar) in the client's `mods` folder.
3. Optionally, put [`MiseryOptimizer-Paper-1.21.11-1.1.0.jar`](jars/MiseryOptimizer-Paper-1.21.11-1.1.0.jar) in a Paper 1.21.11 server's `plugins` folder and restart the server.

The client mod runs without the Paper plugin. Press **O** to open its settings menu. Rebind that key under **Options → Controls → Key Binds → Misery Optimizer**.

## Scope

The client previews crystal placement and break effects, charged-anchor explosions outside the Nether, ender-pearl flight, and nearby explosion terrain. It also sends a pending hotbar slot before mining and tracks local eating animation timing. With the Paper plugin, canceled actions and explosion block lists can correct those previews sooner. See [`fabric/README.md`](fabric/README.md) and [`paper/README.md`](paper/README.md) for behavior, configuration, and test details.

Misery Optimizer does not reduce network latency or server tick time. The server still decides damage, knockback, terrain destruction, food effects, pearl teleportation, and whether an action succeeds. It does not change hit registration or PvP rules.

## Build

From either `fabric/` or `paper/`, run `./gradlew clean build` with JDK 21 or newer. The installable outputs appear in that project's `build/libs/`. The checked-in `jars/` files are the release builds from these sources.

Both projects use the CC0-1.0 license; see their `LICENSE` files.
