# Minecraft 26.3 Fabric build

This folder contains the Minecraft 26.3 client port of Misery Optimizer. Build it with JDK 25 using `./gradlew clean build`. The installable file is `build/libs/misery-optimizer-fabric-26.3-1.5.1.jar`.

Install Fabric Loader 0.19.5 or newer and Fabric API for Minecraft 26.3, then place only `MiseryOptimizer-Fabric-26.3-1.5.1.jar` in the client `mods` folder. Press **O** to open settings, or use **Mod Menu → Misery Optimizer → Configure** when optional Mod Menu is installed. The **Performance...** page offers adaptive effects, a frame budget for terrain scans, adaptive prediction timing, and a diagnostics screen. All four are off by default, as are pearl prediction and terrain preview. Existing `config/tung.json` values are retained when switching versions.

The 1.21.11 client build lives in `../fabric/`. The optional Paper plugin in `../paper/` targets Paper 1.21.11; it is not a 26.3 server plugin.

Verification: the 26.3 source builds and passes its test task. An earlier muted 26.3 Fabric development client reached the main menu with the mod loaded. The new performance options and individual gameplay features were not exercised in a world on 26.3.
